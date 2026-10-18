package de.derpeterson.app.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.LongConsumer;

/**
 * Claim-/Transaktionstests bleiben deterministisch im Aufrufer; Pooltests verwenden echte Threads.
 */
final class QueueTestWorkers {
  private QueueTestWorkers() {}

  static EmailQueueWorkerPool inline() {
    var workers = mock(EmailQueueWorkerPool.class);
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
