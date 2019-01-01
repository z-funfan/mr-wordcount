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
		System.err.printf("Reducer received: <%s>\n", values.toString());

		int sum = 0;
		for (IntWritable value: values) {
			sum = sum + value.get();
		}
		context.write(key, new IntWritable(sum));

	}

}
