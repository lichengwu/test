package oliver.test.algorithm;

import oliver.test.common.BaseTest;
import oliver.test.common.Repeat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * top n test
 *
 * @author 佐井
 * @version 1.0
 * @created 2013-04-19 9:30 AM
 */
public class GetTopN extends BaseTest {

    static long[] DATA;

    static int SIZE = 10000000;

    static int TOP_N = 10;

    static long[] TOP_N_RESULT;

    /**
     * @Before public void init() {
     * long current = System.currentTimeMillis();
     * System.out.println("begin init test class...");
     * <p/>
     * Random rand = new Random(100);
     * DATA = new long[SIZE];
     * <p/>
     * for (int i = 0; i < SIZE; i++) {
     * DATA[i] = rand.nextLong() + 1;
     * }
     * <p/>
     * long[] tmp = DATA.clone();
     * <p/>
     * Arrays.sort(tmp);
     * TOP_N_RESULT = new long[TOP_N];
     * for (int i = TOP_N - 1, iCount = SIZE - 1; i >= 0; i--, iCount--) {
     * TOP_N_RESULT[i] = tmp[iCount];
     * }
     * Runtime.getRuntime().gc();
     * System.out.println("finish init class, time used : " + (System.currentTimeMillis() - current) + "ms");
     * }
     */

    static {
        long current = System.currentTimeMillis();
        System.out.println("begin init test class...");

        Random rand = new Random(100);
        DATA = new long[SIZE];

        for (int i = 0; i < SIZE; i++) {
            DATA[i] = rand.nextLong() + 1;
        }

        long[] tmp = DATA.clone();

        Arrays.sort(tmp);
        TOP_N_RESULT = new long[TOP_N];
        for (int i = TOP_N - 1, iCount = SIZE - 1; i >= 0; i--, iCount--) {
            TOP_N_RESULT[i] = tmp[iCount];
        }
        Runtime.getRuntime().gc();
        System.out.println("finish init class, time used : " + (System.currentTimeMillis() - current) + "ms");
    }


    public static Integer[] getTopN(int[] nums, int topN) {

        final int bitWidth = 32;

        final int SHIFT = 5;

        final int MASK = 0x1F;

        // top n for result
        Integer[] top = new Integer[topN];

        //byte[] bitMap = new byte[nums.length];

        long[] bitMap = new long[1 + Integer.MAX_VALUE / bitWidth];

//        long bitMap = 0;

        int max = Integer.MIN_VALUE;
        for (int i = 0; i < nums.length /*&& iCount < topN*/; i++) {
            //int flag = bitMap[nums[i].intValue()];
//            int mask = 1 << nums[i];
//            if ((bitMap & mask) == 0) {
//                bitMap = bitMap | mask;
//                iCount++;
//            }
            if (max < nums[i]) {
                max = nums[i];
            }
            bitMap[nums[i] >> SHIFT] |= (1 << (i & MASK));
        }

        int index = 0;

        for (int i = max; index < topN; i--) {
            if ((bitMap[nums[i] >> SHIFT] & (1 << (i & MASK))) == 1) {
                top[index] = i;
                index++;
            }
        }
        return top;
    }

    @Test
    public void doNothing() {

    }


    /**
     * get top n number from the given data
     *
     * @param data
     * @param topN
     * @return
     * @author licheng
     */
    public static long[] getTopN1(long[] data, int topN) {
        long[] top = new long[topN];
        // set top items Long.MIN_VALUE
        for (int i = 0; i < topN; i++) {
            top[i] = Long.MIN_VALUE;
        }
        // log max,min value in top
        long max, min = Long.MIN_VALUE;

        for (int i = 0; i < data.length; i++) {
            // skip elements less than top's min element
            if (data[i] <= min) {
                continue;
            } else {
                long tmp = data[i];
                for (int k = top.length - 1; k >= 0; k--) {
                    // skip equal elements
                    if (top[k] == tmp) {
                        break;
                    } else if (tmp > top[k]) { // inset new value to top
                        long current = top[k];
                        top[k] = tmp;
                        tmp = current;
                    }
                }
                // cache max,min value in top
                min = top[0];
                //max = top[topN - 1];
            }
        }
        return top;
    }

    public static long[] getTopN2(final long[] data, final int topN) {

        final long[] top = new long[topN];

        // set top items Long.MIN_VALUE
        for (int i = 0; i < topN; i++) {
            top[i] = Long.MIN_VALUE;
        }
        final ReentrantLock lock = new ReentrantLock(false);

        // max thread 4 running task
        int nThreads = Runtime.getRuntime().availableProcessors() + 1;

        ExecutorService exec = Executors.newFixedThreadPool(nThreads);

        class GetTopTask implements Runnable {

            private int beginIndex;
            private int endIndex;

            GetTopTask(int beginIndex, int endIndex) {
                this.beginIndex = beginIndex;
                this.endIndex = endIndex;
            }

            @Override
            public void run() {
                // log max,min value in top
                long min;
                lock.lock();
                try{
                    min = top[0];
                } finally {
                    lock.unlock();
                }

                for (int i = beginIndex; i < endIndex; i++) {
                    // skip elements less than top's min element
                    if (data[i] <= min) {
                        continue;
                    } else {
                        long tmp = data[i];
                        lock.lock();
                        try {
                            for (int k = top.length - 1; k >= 0; k--) {
                                // skip equal elements
                                if (top[k] == tmp) {
                                    break;
                                } else if (tmp > top[k]) { // insert new value to top
                                    long current = top[k];
                                    top[k] = tmp;
                                    tmp = current;
                                }
                            }
                            // cache max,min value in top
                            min = top[0];
                            //max = top[topN - 1];
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }

        int sub = data.length / nThreads;

        for (int i = 0; i < nThreads; i++) {
            if (i == nThreads - 1) {
                exec.execute(new GetTopTask(i * sub, data.length));
            } else {
                exec.execute(new GetTopTask(i * sub, (i + 1) * sub));
            }
        }
        exec.shutdown();
        return top;
    }


    @Test
    @Repeat(500)
    public void testGetTopN1() {
        Assert.assertArrayEquals(getTopN1(DATA, TOP_N), TOP_N_RESULT);
    }


    @Test
    @Repeat(1)
    public void testGetTopN2() {
        System.out.println(deepToString(getTopN2(new long[]{
                1L, 121L, -1L, 99L, 12L}, 3)));
        //Assert.assertArrayEquals(getTopN2(DATA, TOP_N), TOP_N_RESULT);
    }

    @Test
    public void test() {
        Arrays.sort(DATA);
    }

    private static String deepToString(long[] nums) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nums.length; i++) {
            sb.append(nums[i]);
            if (i == nums.length - 1) {
                sb.append("]");
            } else {
                sb.append(",");
            }
        }

        return sb.toString();

    }

}
