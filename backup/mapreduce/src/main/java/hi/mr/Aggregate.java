package hi.mr;

import java.io.IOException;
import java.util.Arrays;
import hi.mr.proto.Bitcoin.Block;



import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

//import com.hadoop.compression.lzo.LzopCodec;
//http://www.programcreek.com/java-api-examples/index.php?api=com.hadoop.compression.lzo.LzoCodec

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;  
import org.apache.hadoop.hbase.client.Put;  
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;  
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat;  
import org.apache.hadoop.hbase.util.Bytes;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

// JDK API docs : http://docs.oracle.com/javase/7/docs/api/
// Hadoop API docs : http://hadoop.apache.org/docs/stable/api/

public class Aggregate extends Configured implements Tool {

  static class MyMapper extends Mapper<Object, Text, Text, IntWritable> {

    @Override
    public void map(Object key, Text record, Context context)
        throws IOException {
      //System.out.println (record);
      try {
        Block myblock = Block.parseFrom(Base64.decodeBase64(record.toString()));
        context.write(new Text(myblock.getHash()), new IntWritable(1));
      } catch (Exception e) {
        System.out.println("*** exception:");
        e.printStackTrace();
      }
    }
  }

  // public static class MyReducer extends
  //     Reducer<Text, IntWritable, Text, IntWritable> {

  //   public void reduce(Text key, Iterable<IntWritable> results, Context context)
  //       throws IOException, InterruptedException {
  //     int total = 0;
  //     for (IntWritable cost : results) {
  //       total += cost.get();
  //     }
  //     context.write(key, new IntWritable(total));
  //   }
  // }
  public static class MyReducer extends
      TableReducer<Text, IntWritable, ImmutableBytesWritable> {

      public void reduce(ImmutableBytesWritable key, Iterable<IntWritable> values, Context context) throws IOException,
        InterruptedException {
        
        int total = 0;
        for (IntWritable occur : values) {
          total += occur.get();
        }
        Put put = new Put(key.get());
        put.add(Bytes.toBytes("metadata"), Bytes.toBytes("count"), Bytes.toBytes(total));
        context.write(key, put);
      }
  }




  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new Aggregate(), args);
    System.exit(res);
  }

  @Override
  public int run(String[] args) throws Exception {

    // if (args.length != 2) {
    //   System.out.println("usage : need <input path>  <output path>");
    //   return 1;
    // }
    Path inputPath = new Path(args[0]);
    // Path outputPath = new Path(args[1]);

    

    Configuration conf =  HBaseConfiguration.create();
    Job job = new Job(conf,"HDFS-to-HBase");
    job.setJarByClass(Aggregate.class);

    job.setMapperClass(MyMapper.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);
    job.setInputFormatClass(TextInputFormat.class);
    TextInputFormat.setInputPaths(job, inputPath);
    

    TableMapReduceUtil.initTableReducerJob("block_data", MyReducer.class, job);
    job.setReducerClass(MyReducer.class);


    // Configuration conf = getConf();
    // //Configuration conf = HBaseConfiguration.create();

    // Job job = new Job(conf, getClass().getName() + "Bitcoin-Aggregate");
    // job.setJarByClass(Aggregate.class);
    // job.setMapperClass(MyMapper.class);
    // job.setReducerClass(MyReducer.class);
    // job.setMapOutputValueClass(IntWritable.class);
    // job.setMapOutputKeyClass(Text.class);
    // job.setInputFormatClass(TextInputFormat.class);
    // job.setOutputFormatClass(TextOutputFormat.class);
    // TextInputFormat.setInputPaths(job, inputPath);
    // TextOutputFormat.setOutputPath(job, outputPath);

    return job.waitForCompletion(true) ? 0 : 1;
  }

}
