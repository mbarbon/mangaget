/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

public interface PendingTask {
    public void cancel();
    public boolean isCancelled();
}
