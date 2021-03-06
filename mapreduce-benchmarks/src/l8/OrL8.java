/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;

public class OrL8 {

    public static class ReadPageViews extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, Text> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {

            // Split the line
            List<Text> fields = Library.splitLine(val, '');
            if (fields.size() != 9) return;

            StringBuffer sb = new StringBuffer();
            sb.append(fields.get(2).toString());
            sb.append("");
            sb.append(fields.get(6).toString());
            oc.collect(new Text("all"), new Text(sb.toString()));
        }
    }

    public static class Combiner extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            int tsSum = 0, erCnt = 0;
            double erSum = 0.0;
            while (iter.hasNext()) {
            	 List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                    tsSum += Integer.valueOf(vals.get(0).toString());
                    if (vals.size() > 1)
                    erSum += Double.valueOf(vals.get(1).toString());
                    erCnt++;
                } catch (NumberFormatException nfe) {
                }
            }
            StringBuffer sb = new StringBuffer();
            sb.append((new Integer(tsSum)).toString());
            sb.append("");
            sb.append((new Double(erSum)).toString());
            sb.append("");
            sb.append((new Integer(erCnt)).toString());
            oc.collect(key, new Text(sb.toString()));
            reporter.setStatus("OK");
        }
    }
    public static class Group extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            int tsSum = 0, erCnt = 0;
            double erSum = 0.0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                        tsSum += Integer.valueOf(vals.get(0).toString());
	                    erSum += Double.valueOf(vals.get(1).toString());
	                    erCnt += Integer.valueOf(vals.get(2).toString());
                	                    
                } catch (NumberFormatException nfe) {
                }
            }
            double erAvg = erSum / erCnt;
            StringBuffer sb = new StringBuffer();
            sb.append((new Integer(tsSum)).toString());
            sb.append("");
            sb.append((new Double(erAvg)).toString());
            oc.collect(key, new Text(sb.toString()));
            reporter.setStatus("OK");
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length!=2) {
            System.out.println("Parameters: inputDir outputDir");
            System.exit(1);
        }
        String inputDir = args[0];
        String outputDir = args[1];
        JobConf lp = new JobConf(OrL8.class);
        lp.setJobName("L8 Load Page Views");
        lp.setInputFormat(TextInputFormat.class);
        lp.setOutputKeyClass(Text.class);
        lp.setOutputValueClass(Text.class);
        lp.setMapperClass(ReadPageViews.class);
        lp.setCombinerClass(Combiner.class);
        lp.setReducerClass(Group.class);
        Properties props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            lp.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(lp, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(lp, new Path(outputDir + "/L8out"));
        lp.setNumReduceTasks(1);
        Job group = new Job(lp);

        JobControl jc = new JobControl("L8 join");
        jc.addJob(group);

        new Thread(jc).start();
   
        int i = 0;
        while(!jc.allFinished()){
            ArrayList<Job> failures = jc.getFailedJobs();
            if (failures != null && failures.size() > 0) {
                for (Job failure : failures) {
                    System.err.println(failure.getMessage());
                }
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}

            if (i % 10000 == 0) {
                System.out.println("Running jobs");
                ArrayList<Job> running = jc.getRunningJobs();
                if (running != null && running.size() > 0) {
                    for (Job r : running) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Ready jobs");
                ArrayList<Job> ready = jc.getReadyJobs();
                if (ready != null && ready.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Waiting jobs");
                ArrayList<Job> waiting = jc.getWaitingJobs();
                if (waiting != null && waiting.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Successful jobs");
                ArrayList<Job> success = jc.getSuccessfulJobs();
                if (success != null && success.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
            }
            i++;
        }
        ArrayList<Job> failures = jc.getFailedJobs();
        if (failures != null && failures.size() > 0) {
            for (Job failure : failures) {
                System.err.println(failure.getMessage());
            }
        }
        jc.stop();
    }

}
