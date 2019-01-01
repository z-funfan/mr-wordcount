# mr-wordcount

这是我的饿第一个Map-Reduce学习项目
这个简单的例子，将会实现MR程序中简单的应用，完成以下功能：
- 统计输入文章所有单词出现次数
- 输出以字母顺序排序
- 输出两个分区: A~M，N~Z

这个例子中主要讲用到以下类库

- hadoop-core: 实现MR程序
- IKAnalyzer: 实现简单的中文分词

一个最简单的MapReduce程序，至少需要三个部分：
- Mapper阶段：解析，切片，处理；
  - 需要继承MapReduce Mapper类
  - Mapper类需要4个泛型，分别为，TKEYIN;TVALUEIN;TKEYOUT;TVALUEOUT;
  - 这个例子中，输入是文章，即字符串，输出是每个单词的出现次数
  - 分别对应LongWritable（文章切片偏移量）, Text（文章切片字符串）, Tex（单词）, IntWritable （单词出现次数）
- Combiner(可选)：归并
  - 相当于Mapper阶段的本地Reduce方法
  - 一般用于数据量较大的情况下的简单优化
  - 可以减少网络传输的数据量
  - 只有保证归并之后的结果不影响最终输出的情况下，才可用
- Reduce阶段：合并，输出；
  - 当所有Mapper Task完成之后，MR框架会启动Reduce Task，合并所有输出
  - Reducer同样继承MapReduce Reducer类
  - Reducer同样需要4个泛型，TKEYIN;TVALUEIN;TKEYOUT;TVALUEOUT;
  - Mapper的输出，就是Reducer的输入
  - Reduce Task的个数和输出的Partition个数有关  
- Driver：执行任务，返回最终结果

## 搭建Gradle项目

build.gradle
```groovy
apply plugin: 'java'
apply plugin: 'eclipse'

group = 'xyz.funfan'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
	jcenter()
}


dependencies {
	compile group: 'org.apache.hadoop', name: 'hadoop-core', version:'1.2.1'
    
	testCompile 'junit:junit:4.12'
}

```

## 中文分词

这里的中文分词使用IKAnalyzer，最新的IKAnalyzer已经可以独立于lucene使用

```java
	private List<String> segment(String str) throws IOException{
		byte[] byt = str.getBytes();
		InputStream is = new ByteArrayInputStream(byt);
		Reader reader = new InputStreamReader(is);
		IKSegmenter iks = new IKSegmenter(reader, true);
		Lexeme lexeme;
		List<String> list = new ArrayList<String>();
		while((lexeme = iks.next()) != null){
			String text = lexeme.getLexemeText();
			list.add(text);
		}
		return list;
	}
```

## Mapper代码
```java
package xyz.funfan.mr.wordcount;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

public class ArticleWordCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
	// To avoid creating new object frequently
	private final static IntWritable one = new IntWritable(1);
	private Text word = new Text();
	
	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		// Split all words
		String line = value.toString();
        StringTokenizer st = new StringTokenizer(line," ");
        
        // Each word count 1
        // e.g. <a, 1> <b, 1> <c, 1> <a, 1> <a, 1> <z, 1>
        while (st.hasMoreTokens()) {
        	List<String> wordList = this.segment(st.nextToken());
        	for (String wordStr: wordList) {
        		System.err.printf("Mapper found word: <%s: %d>\n", wordStr, one.get());
        		word.set(wordStr);
        		context.write(word, one);
        	}
        }
	}

	private List<String> segment(String str) throws IOException{
		byte[] byt = str.getBytes();
		InputStream is = new ByteArrayInputStream(byt);
		Reader reader = new InputStreamReader(is);
		IKSegmenter iks = new IKSegmenter(reader, true);
		Lexeme lexeme;
		List<String> list = new ArrayList<String>();
		while((lexeme = iks.next()) != null){
			String text = lexeme.getLexemeText();
			list.add(text);
		}
		return list;
	}
}

```

## Partitioner代码
集成Partitioner类，实现自己的分区逻辑。
注意创建Driver类的时候，要制定Reduce Task的个数大于分区数，比如这里，至少要4个Task

```java
package xyz.funfan.mr.wordcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

public class ArticleWordPartitioner extends Partitioner<Text, IntWritable> {

	@Override
	public int getPartition(Text key, IntWritable value, int numPartitions) {
		char capital = key.toString().toLowerCase().charAt(0);
		int partitionNumber = 3;
				
		if (capital < 'a') {
			return 0;
		} else if (capital >= 'a' && capital <= 'm') {
			return 1;
		} else if (capital >= 'n' && capital <= 'z') {
			return 2;
		}
		return partitionNumber;
	}

}


```


## Combiner 代码
未完成

## Reduce代码
```java
package xyz.funfan.mr.wordcount;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class ArticleWordCoutReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

	@Override
	protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
		// The input should be the output of mapper
		// Calculate them together		
        // e.g. [<a, 1> <a, 1> <a, 1> ] or [<b, 1>] or [ <c, 1> ] or [<z, 1>]
		int sum = 0;
		for (IntWritable value: values) {
			sum = sum + value.get();
		}
		context.write(key, new IntWritable(sum));

	}

}

```

## The Driver代码
```java
package xyz.funfan.mr.wordcount;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class ArticleWordCountDriver extends Configured implements Tool {

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new ArticleWordCountDriver(), args);
        System.exit(exitCode);
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.printf("Usage: %s needs two arguments, input and output files\n", getClass().getSimpleName());
			return -1;
		}
	
		// New Job
		Job job = new Job();
		job.setJarByClass(ArticleWordCountDriver.class);
		job.setJobName(getClass().getSimpleName());
		
		// Specified the input and output dir
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
	
		// Set Map-Reduce class
		job.setMapperClass(ArticleWordCountMapper.class);
		job.setReducerClass(ArticleWordCoutReducer.class);
		
		// Set Reduce output format
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		// Set partition
		job.setPartitionerClass(ArticleWordPartitioner.class);
		job.setNumReduceTasks(4);
	
		int returnValue = job.waitForCompletion(true) ? 0:1;
		
		if(job.isSuccessful()) {
			System.out.println("Job was successful");
		} else if(!job.isSuccessful()) {
			System.out.println("Job was not successful");			
		}
		
		return returnValue;
	}
}


```

## 演示效果

运行
```shell
hadoop jar mr-wordcount-0.0.1-SNAPSHOT.jar xyz.funfan.mr.wordcount.ArticleWordCountDriver /article_word_cout/in /article_word_cout/out
```

input.txt
[articles](https://github.com/z-funfan/mr-wordcount/tree/master/articles/input)

part-r-00000
```
"An	1
"Clash	1
"Comparing	1
"Daytona	1
"Furthermore,	1
"Observing	1
"Spark	1
"Through	1
"first	1
"it	1
"run	1
"the	1
"tl;dr"	1
(from	1
(source:	1
-	1
1/10th	1
10/06/2015	1
100TB	1
100x	1
10x	1
12-page	1
1:20	1
2.5x,	1
2014	1
2x	1
3X	1
40GB	2
5x	2
[Click	1
[Resilient	1

```
part-r-00001
```
Academia)!	1
Almaden	1
An	1
Analysis	1
Analytics)	1
Analytics."	1
Apache	2
Apex	1
Article	1
Benchmark.	1
Berthold	1
Besides	1
Big	3
CPU	1
Center,	1
Chen	1
China	1
China,	1
Clash	2
Consider	2
...
```
part-r-00002
```
One	1
Ozcan,	1
PM	1
PageRank	1
PageRank,	1
Performance	1
Posted	1
Provide	1
Proving	1
Qiu,	1
Quora	1
RDD	1
Ramel	1
Reduce	1
Reinwald	1
Renmin	1
Research	2
Scale	2
School	1
Shi,	1
Significantly	1
Smackdown	1
...
```
part-r-00003
```
他说，中美关系40年取得的进展来之不易，其中的历史经验值得汲取。双方要坚持理性客观地看待彼此战略意图，加强战略沟通，增进战略互信，防止战略误判。要坚持中美合作的大方向，不断拓展互利合作领域，更好地惠及两国人民。要坚持尊重彼此主权、安全、发展利益，妥善处理和管控分歧，防止两国关系大局受到干扰。要坚持扩大两国人民交往，不断夯实中美关系的社会基础。	1
外交部发言人就中美建交40周年发表谈话	1
外交部发言人陆慷30日就中美建交40周年发表谈话。	1
来源：人民日报	1
百家号12-3107:50	1
金融界	1
陆慷说，2019年，中美将迎来建交40周年。40年来，中美关系历经风雨，砥砺前行。中美交流与合作取得了历史性的发展。40年前，中美人员往来每年仅几千人次，2017年双方人员往来已超过530万人次。40年前，中美贸易额不足25亿美元，2017年双边贸易额已超过5800亿美元。40年前，中美相互投资几乎为零，2017年两国间各类投资总额累计超过2300亿美元。40年来，从推动地区热点妥善解决到反对国际恐怖主义，从应对国际金融危机到促进全球经济增长，中美在双边、地区、全球层面开展了广泛合作。事实充分证明，中美关系的发展不仅给两国人民带来巨大利益，也有力地促进了亚太地区和世界的和平、稳定、繁荣。	1
陆慷说，经过40年的发展，中美关系已站在新的历史起点上。面对新机遇新挑战，中方愿同美方一道，落实好习近平主席和特朗普总统阿根廷会晤达成的重要共识，在互惠互利基础上拓展合作，在相互尊重基础上管控分歧，推进以协调、合作、稳定为基调的中美关系，让中美合作更多更好地造福两国人民和世界各国人民。	1

...
```
