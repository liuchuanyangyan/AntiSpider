package org.outstudio.antispider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 
 * @author liuch
 *
 */
public class AntiSpider {
	private final Map<String, List<Integer>> visitTimeListMap = new ConcurrentHashMap<>();
	private final AntiHandler antiHandler;
	private final Map<Integer, Integer> spaceAndTimesMap;
	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * 构造方法
	 * 
	 * @param antiHandler
	 *            判定有爬虫行为时的处理器
	 * @param spaceAndTimesMap
	 *            判定规则，当key秒内访问val次即视为爬虫
	 */
	public AntiSpider(AntiHandler antiHandler, Map<Integer, Integer> spaceAndTimesMap) {
		this.antiHandler = antiHandler;
		this.spaceAndTimesMap = Collections.unmodifiableMap(new HashMap<>(spaceAndTimesMap));
	}

	/**
	 * 当用户访问时，手动调用该方法
	 * 
	 * @param key
	 *            区分用户的唯一标识
	 * @throws ExecutionException
	 *             处理爬虫时抛出的Exception
	 */
	public void log(String key) throws ExecutionException {
		List<Integer> theList = visitTimeListMap.putIfAbsent(key, new ArrayList<>());
		Integer time = (int) (System.currentTimeMillis() / 1000);
		synchronized (theList) {
			theList.add(time);
			anti(key);
		}
	}

	/**
	 * 
	 * @param key
	 *            区分用户的唯一标识
	 * @throws ExecutionException
	 *             处理爬虫时抛出的Exception
	 */
	private void anti(String key) throws ExecutionException {
		for (Entry<Integer, Integer> entry : spaceAndTimesMap.entrySet()) {
			Integer space = entry.getKey();
			Integer times = entry.getValue();
			List<Integer> theList = visitTimeListMap.get(key);
			if (theList.size() - times > 0) {
				Integer pre = theList.get(theList.size() - times - 1);
				Integer now = theList.get(theList.size() - 1);
				if (pre - now <= space) {
					Future<?> f = executor.submit(new Runnable() {
						public void run() {
							antiHandler.doHandle(key);
						}
					});
					try {
						f.get();
					} catch (InterruptedException e) {
						f.cancel(true);
						e.printStackTrace();
					} catch (ExecutionException e) {
						throw e;
					}
				}
			}
		}
	}

	/**
	 * 清理所有记录
	 */
	public void clear() {
		this.visitTimeListMap.clear();
	}

	/**
	 * 关闭该服务
	 */
	public void shutdown() {
		this.executor.shutdown();
	}
}
