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
