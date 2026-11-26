package me.dani.lookout.rum;

public record IngestResult(int received, int accepted, int stored) {

    public int dropped() {
        return received - accepted;
    }

    public int duplicates() {
        return accepted - stored;
    }
}
