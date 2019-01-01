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
