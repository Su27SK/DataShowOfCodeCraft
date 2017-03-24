package com.huai.web.service.impl;

import com.huai.web.pojo.*;
import com.huai.web.service.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by liangyh on 3/14/17.
 */
@Component
public class ExecutionServiceImpl implements ExecutionService{

    public Result judgeResultSet(InputStream inputOfDataSet, InputStream inputOfResultSet){
        Result result = new Result();

        int totalCost = 0;
        int basicCost = 0;
        //存储服务器节点的id
        Set<Integer> serverDeployed = new HashSet<Integer>();

        BufferedReader reader = null;
        try{
            DataSet dataset = parseDataSet(inputOfDataSet);
            Graph graph = new Graph(dataset);

            reader = new BufferedReader(new InputStreamReader(inputOfResultSet));
            int pathNum = Integer.valueOf(reader.readLine().trim());

            for(int i = 0; i < pathNum; i++){
                String line = reader.readLine();
                if(line.trim().length() < 1){
                    i--;
                    continue;
                }

                String[] lineSplited = line.split(" ");
                if(lineSplited.length < 3)return result;

                serverDeployed.add(Integer.valueOf(lineSplited[0]));

                int flow = Integer.valueOf(lineSplited[lineSplited.length-1]);
                String nodeNearConsumer = lineSplited[lineSplited.length-3];
                if(flow <= 0)return result;
                for(int k = 0; k < lineSplited.length-3; k++){
                    //更新每一个网络链路的容量， 如果小于0，就false；
                    Vertex currVertex = graph.getMatrix().get(lineSplited[k]).get(lineSplited[k+1]);
                    int leftVolume = currVertex.getVolume()-flow;
                    if(leftVolume < 0)return result;
                    currVertex.setVolume(leftVolume);

                    //统计链路流量费用
                    int flowCostOfTheLink = currVertex.getCost()*flow;
                    totalCost += flowCostOfTheLink;
                }
                //更新消费节点的需求
                if(!graph.getNetworkToConsumer().containsKey(nodeNearConsumer)) return result;

                Consumer currConsumer = graph.getNetworkToConsumer().get(nodeNearConsumer);

                if(!currConsumer.getId().equals(getConsumerId(lineSplited[lineSplited.length-2])))return result;

                currConsumer.setRequirement(currConsumer.getRequirement()-flow);
            }
            //check consumer requirement
            for(Consumer consumer: graph.getNetworkToConsumer().values()){
                if(consumer.getRequirement() > 0)return result;
            }
            //统计服务器总费用
            totalCost += (serverDeployed.size()*dataset.getServerDeployCost());
            basicCost = graph.getNetworkToConsumer().size()*dataset.getServerDeployCost();
        }catch (Exception e){
            e.printStackTrace();
            return result;
        }
        result.setGood(true);
        result.setData("The total cost of your algorithm is: "+totalCost+"; the basic cost is: "+basicCost);
        return result;
    }


    public DataSet parseDataSet(InputStream inputStream){
        Set<Relationship> relationships = new HashSet<Relationship>();
        Set<Consumer> consumers = new HashSet<Consumer>();
        int serverDeployCost = 0;
        BufferedReader br = null;
        int networkNum = 0;
        int linksNum = 0;
        int consumerNum = 0;

        try {
            br = new BufferedReader(new InputStreamReader(inputStream));

            String[] tempStrs = br.readLine().split(" ");
            networkNum = Integer.valueOf(tempStrs[0]);
            linksNum = Integer.valueOf(tempStrs[1]);
            consumerNum = Integer.valueOf(tempStrs[2]);

            br.readLine();

            serverDeployCost = Integer.valueOf(br.readLine().trim());

            br.readLine();

            for(int i = 0; i < linksNum; i++){
                String[] strs = br.readLine().split(" ");

                String startNetworkId = strs[0];
                String endNetworkId = strs[1];
                int volume = Integer.valueOf(strs[2]);
                int cost = Integer.valueOf(strs[3]);

                Relationship relationship = new Relationship(startNetworkId, endNetworkId, "["+volume+"/"+cost+"]", volume, cost);
                relationships.add(relationship);
            }

            br.readLine();

            for(int i = 0; i < consumerNum; i++){
                String[] strs = br.readLine().split(" ");
                String consumerID = getConsumerId(strs[0]);
                String networkId = strs[1];
                int requirement = Integer.valueOf(strs[2]);

                Relationship relationship = new Relationship(consumerID, ""+networkId, "["+requirement+"]", 1, 0);
                Consumer consumer = new Consumer(consumerID, networkId, requirement);
                relationships.add(relationship);
                consumers.add(consumer);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if(br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        List<Node> nodeList = new ArrayList<Node>();
        for(int i = 0; i < networkNum; i++){
            Node newNode = new Node(1, ""+i, 1);
            nodeList.add(newNode);
        }

        for(int i = 0; i < consumerNum; i++){
            nodeList.add(new Node(2, i+".", 2));
        }

        DataSet dataset = new DataSet(relationships, consumerNum, networkNum);
        dataset.setNodeList(nodeList);
        dataset.setConsumerSet(consumers);
        dataset.setServerDeployCost(serverDeployCost);
        return dataset;
    }

    private String getConsumerId(String originalId){
        return originalId+".";
    }

}
