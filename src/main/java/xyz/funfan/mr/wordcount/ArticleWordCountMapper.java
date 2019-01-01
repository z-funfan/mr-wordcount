package xyz.funfan.mr.wordcount;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

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
			System.err.printf("Mapper found word: <%s: %d>\n", word.toString(), one.get());
        	word.set(st.nextToken());
        	context.write(word, one);
        }
	}

}
