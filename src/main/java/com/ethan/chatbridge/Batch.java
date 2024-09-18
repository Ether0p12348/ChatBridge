package com.ethan.chatbridge;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

public class Batch {
    private String id = "batch_" + ChatBridge.genId(8);
    private final List<Payload> payloads = new ArrayList<>();

    public Batch (@Nullable String id, Payload... payload) {
        this.id = id != null ? id : this.id;
        payloads.addAll(Arrays.asList(payload));
    }

    public Batch(){}

    public Batch setId (String id) {
        this.id = id;
        return this;
    }

    public Batch add (Payload payload) {
        this.payloads.add(payload);
        return this;
    }

    public Batch remove (String id) {
        payloads.removeIf(payload -> payload.getId().equals(id));
        return this;
    }

    public String getId() {
        return this.id;
    }

    public List<Payload> getPayloads() {
        return this.payloads;
    }

    public Payload get (String id) {
        for (Payload p : this.payloads) {
            if (p.getId().equals(id)) {
                return p;
            }
        }

        return null;
    }

    public Batch queue (long delay) {
        System.out.println("Beginning batch: " + this.id);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Map<Payload, CompletableFuture<String>> futures = new HashMap<>();

        for (int i = 0; i < payloads.size(); i++) {
            CompletableFuture<String> future = new CompletableFuture<>();
            final int in = i;
            scheduler.schedule(() -> {
                payloads.get(in).translateAsync().whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(result);
                    }
                });
            }, i * delay, TimeUnit.SECONDS);

            futures.put(payloads.get(i), future);
        }

        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        scheduler.shutdown();

        System.out.println("Batch completed: " + this.id);

        return this;
    }
}
