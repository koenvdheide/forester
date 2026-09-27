package org.forester.archaeopteryx;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.ws.seqdb.TaxonomicLineageService;
import org.forester.ws.seqdb.TaxonLineage;

/** A held taxonomy lookup cannot revive a closed embedded viewer. */
public final class EmbeddedWorkerCloseTest {

    public static void main(final String[] args) {
        try {
            exercise();
            System.out.println("EMBEDDED_WORKER_CLOSE_OK");
            System.exit(0);
        } catch (final Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void exercise() throws Exception {
        final CountDownLatch arrived = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger completions = new AtomicInteger();
        final AtomicInteger fetches = new AtomicInteger();
        final TaxonomicLineageService held = new TaxonomicLineageService() {
            @Override
            public TaxonLineage lineageOf(final String taxon) {
                return null;
            }

            @Override
            public TaxonLineage fetch(final String taxon) throws IOException {
                fetches.incrementAndGet();
                if (!"held".equals(taxon)) {
                    return TaxonLineage.EMPTY;
                }
                arrived.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IOException("Response was not released");
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                return TaxonLineage.EMPTY;
            }
        };
        final Field service = TreePanelUtil.class.getDeclaredField("_default_lineage_service");
        service.setAccessible(true);
        final Object original = service.get(null);
        final JFrame[] host = new JFrame[1];
        final MainFrameApplication[] viewer = new MainFrameApplication[1];
        final OnlineTaxonResolver[] resolver = new OnlineTaxonResolver[1];
        final List<String> failures = new ArrayList<>();
        Thread worker = null;
        try {
            service.set(null, held);
            SwingUtilities.invokeAndWait(() -> {
                final Phylogeny phy = new Phylogeny();
                final PhylogenyNode root = new PhylogenyNode();
                root.addAsChild(new PhylogenyNode("a"));
                root.addAsChild(new PhylogenyNode("b"));
                phy.setRoot(root);
                phy.externalNodesHaveChanged();
                viewer[0] = MainFrameApplication.createEmbeddedInstance(new Phylogeny[] { phy },
                        new Configuration(), "close worker");
                host[0] = new JFrame("host");
                host[0].setContentPane(viewer[0]);
                host[0].setJMenuBar(viewer[0].getJMenuBar());
                host[0].setSize(800, 600);
                host[0].setVisible(true);
                resolver[0] = new OnlineTaxonResolver(viewer[0], "held lookup",
                        new TreeSet<>(Collections.singleton("held")), error -> completions.incrementAndGet());
            });
            final AtomicInteger cancelledCompletions = new AtomicInteger();
            final OnlineTaxonResolver cancelled = new OnlineTaxonResolver(viewer[0], "cancelled lookup",
                    new TreeSet<>(Collections.singleton("cancelled")), error -> cancelledCompletions.incrementAndGet());
            cancelled.requestCancel();
            cancelled.run();
            SwingUtilities.invokeAndWait(() -> {
                if (cancelledCompletions.get() != 1 || fetches.get() != 0) {
                    throw new AssertionError("Manual cancellation on an open viewer must report completion without fetching");
                }
            });
            worker = new Thread(resolver[0], "held-taxonomy-test");
            worker.start();
            if (!arrived.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Lookup did not start");
            }
            SwingUtilities.invokeAndWait(() -> {
                if (!viewer[0].isProcessAnimationRunningForTest()) {
                    throw new AssertionError("The held worker must start the process timer");
                }
                final OnlineTaxonResolver quick = new OnlineTaxonResolver(viewer[0], "quick lookup",
                        new TreeSet<>(Collections.singleton("quick")), error -> completions.incrementAndGet());
                final Thread quickWorker = new Thread(quick, "quick-taxonomy-test");
                quickWorker.start();
                // Hold the EDT so completion and menu updates queue before close.
                try {
                    quickWorker.join(10_000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                if (quickWorker.isAlive()) {
                    throw new AssertionError("Quick lookup did not finish while the EDT was held");
                }
                viewer[0].end();
                viewer[0].end();
                if (!resolver[0].isCancelled()) {
                    failures.add("Closing must cancel the registered lookup");
                }
                if (viewer[0].isProcessAnimationRunningForTest()) {
                    failures.add("Closing must stop the process timer");
                }
                if (!host[0].isDisplayable()) {
                    failures.add("Embedded close disposed the host");
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                if (viewer[0].getProcessPool().size() != 1) {
                    throw new AssertionError("Held lookup must remain registered until response release");
                }
                if (completions.get() != 0) {
                    failures.add("Closed viewer received a completion queued before close");
                }
                if (viewer[0].isProcessAnimationRunningForTest()) {
                    failures.add("A queued process update restarted the timer while a lookup was held");
                }
            });
            release.countDown();
            worker.join(10_000);
            if (worker.isAlive()) {
                throw new AssertionError("Lookup did not complete after response release");
            }
            SwingUtilities.invokeAndWait(() -> {
                if (completions.get() != 0) {
                    failures.add("Closed viewer received a late completion");
                }
                if (viewer[0].getProcessPool().size() != 0) {
                    failures.add("Finished lookup remains registered");
                }
            });
            final OnlineTaxonResolver late = new OnlineTaxonResolver(viewer[0], "late lookup",
                    new TreeSet<>(Collections.singleton("late")), error -> completions.incrementAndGet());
            final Thread lateWorker = new Thread(late, "late-taxonomy-test");
            lateWorker.start();
            lateWorker.join(10_000);
            if (lateWorker.isAlive()) {
                throw new AssertionError("Late lookup did not stop");
            }
            SwingUtilities.invokeAndWait(() -> {
                if (!late.isCancelled() || fetches.get() != 2 || completions.get() != 0) {
                    failures.add("Work starting after close must not fetch or update UI");
                }
            });
            if (!failures.isEmpty()) {
                throw new AssertionError(String.join("; ", failures));
            }
        } finally {
            release.countDown();
            if (worker != null) {
                worker.join(10_000);
            }
            SwingUtilities.invokeAndWait(() -> {
                if (viewer[0] != null) {
                    viewer[0].end();
                }
                if (host[0] != null) {
                    host[0].dispose();
                }
            });
            service.set(null, original);
        }
    }
}
