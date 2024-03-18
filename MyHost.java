/* Implement this class. */

import java.util.concurrent.PriorityBlockingQueue;

class ComparableTask implements Comparable<ComparableTask> {
    private final int priority;
    private final Task task;
    private final int start;

    public ComparableTask(Task task) {
        this.task = task;
        this.priority = task.getPriority();
        this.start = task.getStart();
    }

    public Task getTask() {
        return task;
    }

    public int getStart() {
        return start;
    }

    @Override
    public int compareTo(ComparableTask other) {
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison == 0) {
            return Integer.compare(this.start, other.start);
        }
        return priorityComparison;
    }
}

public class MyHost extends Host {
    /**
     * RUNTIME_SLEEP is the amount of time the host "runs" a task for each iteration of the while loop
     * in the run() method. It is in seconds.
     */
    private static final double RUNTIME_SLEEP = 0.1;
    /**
     * the queue of tasks waiting to be run by the host
     * the queue is a priority queue, sorted by priority and then by start time
     */
    private final PriorityBlockingQueue<ComparableTask> tasks = new PriorityBlockingQueue<>();
    /**
     * lastCheckTime is the last time we checked the time left for the current task
     */
    private double lastCheckTime;
    /**
     * isRunning is true if the host is currently running a task
     */
    private boolean isRunning = false;
    /**
     * isShutDown is true if the host has been told to shut down
     */
    private boolean isShutDown = false;
    /**
     * the task that the host is currently running
     */
    private Task currentTask;

    /**
     * Decreases the time left for the current task by the time passed
     * since the last time we updated its time left.
     * If the task is finished (aka time left is 0 or less),
     * we call finish() on the task
     */
    public void decreaseCount() {
        currentTask.setLeft((long) (currentTask.getLeft() - 1000 * (Timer.getTimeDouble() - lastCheckTime)));
        lastCheckTime = Timer.getTimeDouble();
        if (currentTask.getLeft() <= 0) {
            isRunning = false;
            currentTask.finish();
            currentTask = null;
            //System.out.println("Task " + currentTask.getId() + " finished at time " + lastCheckTime);
        }
    }

    /**
     * Checks if the task can be started (more time has passed then the start time of the task),
     * and if so, starts it. Technically it's not required, but better safe than sorry.
     * Also removes the task from the queue.
     *
     * @param comparableTask the task to be checked
     * @return true if the task was started, false otherwise
     */
    public boolean canStart(ComparableTask comparableTask) {
        Task task = comparableTask.getTask();
        if (Timer.getTimeDouble() >= task.getStart()) {
            lastCheckTime = Timer.getTimeDouble();
            currentTask = task;
            // result of remove can be ignored, since we won't even call this function
            // if the task is not in the queue
            tasks.remove(comparableTask);
            isRunning = true;
            //System.out.println("Task " + task.getId() + " started at time " + lastCheckTime);
            return true;
        }
        return false;
    }

    /**
     * Checks if the task can be preempted, and if so, preempts it.
     * Also removes the task from the queue, and adds the current task to the queue.
     *
     * @param comparableTask the task to be checked
     * @return true if the task was preempted, false otherwise
     */
    public boolean checkForPreempt(ComparableTask comparableTask) {
        Task task = comparableTask.getTask();
        if (task.getPriority() > currentTask.getPriority() && Timer.getTimeDouble() >= task.getStart()) {
            decreaseCount();
            // result of remove can be ignored, since we won't even call this function
            // if the task is not in the queue
            tasks.remove(comparableTask);
            addTask(currentTask);
            currentTask = task;
            lastCheckTime = Timer.getTimeDouble();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        // continue running until we are told to shut down and there is no more work to be done
        while (true) {
            // if we are told to shut down and there is no more work to be done, shut down
            if (isShutDown && tasks.isEmpty() && !isRunning) {
                return;
            }
            if (!isRunning) { // if we are not running a task, check if we can start one
                try {
                    // because we use a priority queue, the thread will block until there is a task to be run
                    // which ensures proper synchronization
                    ComparableTask comparableTask = tasks.take();
                    if (canStart(comparableTask)) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    // we can only be interrupted if we are told to shut down
                    // if we are told to shut down and there is no more work to be done, shut down
                    if (isShutDown && tasks.isEmpty() && !isRunning) {
                        return;
                    }
                }
                // go through all tasks until we find one that can be started
                for (ComparableTask comparableTask : tasks) {
                    if (canStart(comparableTask)) {
                        break;
                    }
                }
            } else {
                // if we are running a task, check if we can preempt it for another one
                if (currentTask.isPreemptible()) {
                    for (ComparableTask comparableTask : tasks) {
                        if (checkForPreempt(comparableTask)) {
                            break;
                        }
                    }
                }
                // "run" the task
                try {
                    sleep((long) (RUNTIME_SLEEP * 1000));
                    decreaseCount();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Adds a task to the queue.
     * The queue automatically sorts the tasks by priority and then by start time.
     *
     * @param task task to be added at this host
     */
    @Override
    public void addTask(Task task) {
        tasks.put(new ComparableTask(task));
    }

    /**
     * Gets the number of tasks in the queue.
     *
     * @return number of tasks in the queue
     */
    @Override
    public int getQueueSize() {
        return tasks.size();
    }

    /**
     * Gets the total work left for the host
     *
     * @return total work left in the queue and in the current task
     */
    @Override
    public long getWorkLeft() {
        long workLeft = 0;
        for (ComparableTask comparableTask : tasks) {
            workLeft += comparableTask.getTask().getLeft();
        }
        if (isRunning) {
            workLeft += currentTask.getLeft();
        }
        return workLeft;
    }

    /**
     * Tells the host to shut down.
     * If the host is not running and the queue is empty, interrupts the thread,
     * which will cause the host to shut down.
     */
    @Override
    public void shutdown() {
        isShutDown = true;
        if (!isRunning && tasks.isEmpty()) {
            interrupt();
        }
    }
}
