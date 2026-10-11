package dev.viktorstrohush.orderflow.order.integration;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate passes every SQL statement through here before running it. It keeps only those of
 * the thread that asked to record, so the outbox relay, which queries in the background, does not
 * get into the count. Hibernate creates the instance itself (see the property in the test).
 */
public class SqlRecorder implements StatementInspector {

    private static final ThreadLocal<List<String>> RECORDING = new ThreadLocal<>();

    /** Runs the action and returns the SQL it executed in this thread. */
    static List<String> record(Runnable action) {
        List<String> statements = new ArrayList<>();
        RECORDING.set(statements);
        try {
            action.run();
        } finally {
            RECORDING.remove();
        }
        return statements;
    }

    @Override
    public String inspect(String sql) {
        List<String> statements = RECORDING.get();
        if (statements != null) {
            statements.add(sql);
        }
        return sql;
    }
}
