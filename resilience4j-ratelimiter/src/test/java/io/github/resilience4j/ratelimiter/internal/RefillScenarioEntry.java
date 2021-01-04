package io.github.resilience4j.ratelimiter.internal;

public class RefillScenarioEntry {

    private Long nano;
    private int bucketBefore;
    private int request;
    private int bucketAfter;
    private boolean result;

    public RefillScenarioEntry(Long nano, int bucketBefore, int request, int bucketAfter, boolean result) {
        this.nano = nano;
        this.bucketBefore = bucketBefore;
        this.request = request;
        this.bucketAfter = bucketAfter;
        this.result = result;
    }

    public Long getNano() {
        return nano;
    }

    public void setNano(Long nano) {
        this.nano = nano;
    }

    public int getBucketBefore() {
        return bucketBefore;
    }

    public void setBucketBefore(int bucketBefore) {
        this.bucketBefore = bucketBefore;
    }

    public int getRequest() {
        return request;
    }

    public void setRequest(int request) {
        this.request = request;
    }

    public int getBucketAfter() {
        return bucketAfter;
    }

    public void setBucketAfter(int bucketAfter) {
        this.bucketAfter = bucketAfter;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

}
