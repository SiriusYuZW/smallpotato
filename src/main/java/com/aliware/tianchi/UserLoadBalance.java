package com.aliware.tianchi;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.Random;

/**
 * @author xiaotudou
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {


    public static final String NAME = "user_load";
    private final Random random = new Random();

    public UserLoadBalance() {
    }

    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        int totalWeight = 0;
        boolean sameWeight = true;

        int offset;
        int i;
        for(offset = 0; offset < length; ++offset) {
            i = this.getWeight((Invoker)invokers.get(offset), invocation);
            totalWeight += i;
            if (sameWeight && offset > 0 && i != this.getWeight((Invoker)invokers.get(offset - 1), invocation)) {
                sameWeight = false;
            }
        }

        if (totalWeight > 0 && !sameWeight) {
            offset = this.random.nextInt(totalWeight);

            for(i = 0; i < length; ++i) {
                offset -= this.getWeight((Invoker)invokers.get(i), invocation);
                if (offset < 0) {
                    return (Invoker)invokers.get(i);
                }
            }
        }

        return (Invoker)invokers.get(this.random.nextInt(length));
    }

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), "weight", 100);
        if (weight > 0) {
            long timestamp = invoker.getUrl().getParameter("timestamp", 0L);
            if (timestamp > 0L) {
                int uptime = (int)(System.currentTimeMillis() - timestamp);
                int warmup = invoker.getUrl().getParameter("warmup", 600000);
                if (uptime > 0 && uptime < warmup) {
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }

        return weight;
    }

    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        int ww = (int)((float)uptime / ((float)warmup / (float)weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }
}
