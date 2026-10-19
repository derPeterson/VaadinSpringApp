package de.derpeterson.app.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.function.LongConsumer;

/**
 * Claim-/Transaktionstests bleiben deterministisch im Aufrufer; Pooltests verwenden echte Threads.
 */
final class QueueTestWorkers {
  private QueueTestWorkers() {}

  static EmailQueueWorkerPool inline() {
    EmailQueueWorkerPool workers = mock(EmailQueueWorkerPool.class);
    lenient()
        .doAnswer(
            call -> {
              List<Long> ids = call.getArgument(0);
              LongConsumer dispatch = call.getArgument(1);
              ids.forEach(dispatch::accept);
              return null;
            })
        .when(workers)
        .process(anyList(), any());
    return workers;
  }
}
