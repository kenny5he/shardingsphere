/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.underlying.executor.constant;

/**
 * Connection Mode. 连接模式
 *
 * 参考 Blog: https://segmentfault.com/a/1190000023337951
 *
 * 从资源控制的角度看，业务方访问数据库的连接数量应当有所限制。
 * 它能够有效地防止某一业务操作过多的占用资源，从而将数据库连接的资源耗尽，以致于影响其他业务的正常访问。
 * 特别是在一个数据库实例中存在较多分表的情况下，一条不包含分片键的逻辑SQL将产生落在同库不同表的大量真实SQL，如果每条真实SQL都占用一个独立的连接，
 * 那么一次查询无疑将会占用过多的资源。
 *
 * 从执行效率的角度看，为每个分片查询维持一个独立的数据库连接，可以更加有效的利用多线程来提升执行效率。
 * 为每个数据库连接开启独立的线程，可以将I/O所产生的消耗并行处理。为每个分片维持一个独立的数据库连接，还能够避免过早的将查询结果数据加载至内存。
 * 独立的数据库连接，能够持有查询结果集游标位置的引用，在需要获取相应数据时移动游标即可。
 *
 * 以结果集游标下移进行结果归并的方式，称之为流式归并，它无需将结果数据全数加载至内存，可以有效的节省内存资源，进而减少垃圾回收的频次。
 * 当无法保证每个分片查询持有一个独立数据库连接时，则需要在复用该数据库连接获取下一张分表的查询结果集之前，将当前的查询结果集全数加载至内存。
 * 因此，即使可以采用流式归并，在此场景下也将退化为内存归并。
 *
 * 作用：
 *      一方面是对数据库连接资源的控制保护，一方面是采用更优的归并模式达到对中间件内存资源的节省
 *
 * 适用场景:
 *      内存限制模式: 适用于OLAP操作，可以通过放宽对数据库连接的限制提升系统吞吐量；
 *      连接限制模式: 适用于OLTP操作，OLTP通常带有分片键，会路由到单一的分片，因此严格控制数据库连接，以保证在线系统数据库资源能够被更多的应用所使用，是明智的选择。
 *
 * 数据处理:
 *      联机事务处理OLTP（on-line transaction processing）:
 *          1. 使用范围: 传统的关系型数据库的主要应用，主要是基本的、日常的事务处理
 *          2. 系统强调数据库内存效率，强调内存各种指标的命令率，强调绑定变量，强调并发操作
 *      联机分析处理OLAP（On-Line Analytical Processing）:
 *          1. 使用范围: 数据仓库系统的主要应用，支持复杂的分析操作，侧重决策支持，并且提供直观易懂的查询结果。
 *          2. 系统则强调数据分析，强调SQL执行市场，强调磁盘I/O，强调分区等。
 */
public enum ConnectionMode {
    /*
     * 内存限制模式:
     *      前提:  ShardingSphere对一次操作所耗费的数据库连接数量不做限制。
     *      如果实际执行的SQL需要对某数据库实例中的200张表做操作，则对每张表创建一个新的数据库连接，并通过多线程的方式并发处理，
     *      以达成执行效率最大化。并且在SQL满足条件情况下，优先选择流式归并，以防止出现内存溢出或避免频繁垃圾回收情况
     */
    MEMORY_STRICTLY,
    /*
     * 连接限制模式:
     *      前提: ShardingSphere严格控制对一次操作所耗费的数据库连接数量。
     *      如果实际执行的SQL需要对某数据库实例中的200张表做操作，那么只会创建唯一的数据库连接，并对其200张表串行处理。
     *      如果一次操作中的分片散落在不同的数据库，仍然采用多线程处理对不同库的操作，但每个库的每次操作仍然只创建一个唯一的数据库连接。
     *      这样即可以防止对一次请求对数据库连接占用过多所带来的问题。该模式始终选择内存归并。
     */
    CONNECTION_STRICTLY
}
