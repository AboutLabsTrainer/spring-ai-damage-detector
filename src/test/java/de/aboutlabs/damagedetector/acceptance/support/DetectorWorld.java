package de.aboutlabs.damagedetector.acceptance.support;

/** Test composition root, one per scenario: wires the services the step methods delegate to. */
public final class DetectorWorld {

    private final Workspace folders = new Workspace();
    private final JevStub jev = new JevStub(folders);
    private final FakeCapabilities capabilities = new FakeCapabilities();
    private final DetectorRun run = new DetectorRun(folders, jev, capabilities);
    private final BatchRecord batch = new BatchRecord(folders, run);
    private final EvaluationRecord evaluation = new EvaluationRecord(folders, run);
    private final ContainerCheck container = new ContainerCheck();

    public Workspace folders() {
        return folders;
    }

    public JevStub jev() {
        return jev;
    }

    public FakeCapabilities capabilities() {
        return capabilities;
    }

    public DetectorRun run() {
        return run;
    }

    public BatchRecord batch() {
        return batch;
    }

    public EvaluationRecord evaluation() {
        return evaluation;
    }

    public ContainerCheck container() {
        return container;
    }

    public void open() {
        jev.start();
    }

    public void close() {
        jev.stop();
        folders.remove();
    }
}
