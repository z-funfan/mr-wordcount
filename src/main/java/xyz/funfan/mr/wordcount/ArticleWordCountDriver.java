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
			System.out.println("Job was failed");			
		}
		
		return returnValue;
	}

}
