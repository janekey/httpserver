package com.janekey.http.net;

/**
 * 三元组
 * User: p_qizheng
 * Date: 14-11-18
 * Time: 下午3:36
 */
public class Ternary<F, S, T> {

    public F first;
    public S second;
    public T third;

    public Ternary(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

}
