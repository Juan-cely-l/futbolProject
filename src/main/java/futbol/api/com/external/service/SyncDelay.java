package futbol.api.com.external.service;

@FunctionalInterface
interface SyncDelay {

    void sleep();

    static SyncDelay defaultDelay() {
        return () -> {
            try {
                Thread.sleep(SyncConstants.DELAY_BETWEEN_TEAMS_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
