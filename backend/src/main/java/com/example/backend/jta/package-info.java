/**
 * Distributed transactions (F14): {@code investment} rows use the primary PostGIS datasource.
 * Atomikos {@code transactions-jta} / {@code transactions-jdbc} are available on the classpath for a
 * future dual-resource setup (two {@link javax.sql.XADataSource} pools + JTA user transaction +
 * separate persistence units) when a second catalog participates in two-phase commit.
 */
package com.example.backend.jta;
