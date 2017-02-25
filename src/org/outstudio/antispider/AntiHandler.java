package org.outstudio.antispider;

/**
 * 
 * @author liuch
 */
public interface AntiHandler {

	/**
	 * 判定有爬虫行为时会回调该函数
	 * 
	 * @param s
	 *            区分用户的唯一标识
	 */
	public void doHandle(String s);
}
