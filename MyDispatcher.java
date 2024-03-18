/* Implement this class. */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyDispatcher extends Dispatcher {
    /**
     * robinLastID is the ID of the last host that was assigned a task.
     * This is used for RR scheduling.
     * It is initialized to -1, so it flows smoothly without ifs and branch predictions,
     * since we always increment the ID before using it
     */
    private int robinLastID = -1;

    public MyDispatcher(SchedulingAlgorithm algorithm, List<Host> hosts) {
        super(algorithm, hosts);
    }

    /**
     * Adds a tasks to this dispatcher, which assigns it to one of the hosts
     * based on the scheduling algorithm of the dispatcher.
     *
     * @param task task to be scheduled
     */
    @Override
    public void addTask(Task task) {
        //System.out.println(task);
        switch (algorithm) {
            case ROUND_ROBIN:
                // increment the ID and get the host with that ID
                // as per the RR scheduling algorithm
                robinLastID = (robinLastID + 1) % hosts.size();
                MyHost host = (MyHost) hosts.get(robinLastID);
                // add the task to the host
                host.addTask(task);
                break;
            case SHORTEST_QUEUE:
                // copy the hosts list into a new list that uses MyHost instead of Host
                List<MyHost> sortedHostsSQ = new ArrayList<>();
                for (Host h : hosts) {
                    sortedHostsSQ.add((MyHost) h);
                }
                // get the host with the shortest queue size and the least work left
                MyHost minHostSQ = Collections.min(sortedHostsSQ, (o1, o2) -> {
                    int queueSizeComparison = Integer.compare(o1.getQueueSize(), o2.getQueueSize());
                    if (queueSizeComparison == 0) {
                        int workLeftComparison = Double.compare(o1.getWorkLeft(), o2.getWorkLeft());
                        if (workLeftComparison == 0) {
                            return Integer.compare((int) o1.getId(), (int) o2.getId());
                        }
                        return workLeftComparison;
                    }
                    return queueSizeComparison;
                });
                // add the task to the host
                minHostSQ.addTask(task);
                break;
            case SIZE_INTERVAL_TASK_ASSIGNMENT:
                // see which type of task it is and add it to the appropriate host
                // as per the SITA algorithm
                switch (task.getType()) {
                    case SHORT:
                        MyHost short_host = (MyHost) hosts.get(0);
                        short_host.addTask(task);
                        break;
                    case MEDIUM:
                        MyHost medium_host = (MyHost) hosts.get(1);
                        medium_host.addTask(task);
                        break;
                    case LONG:
                        MyHost long_host = (MyHost) hosts.get(2);
                        long_host.addTask(task);
                        break;
                }
                break;
            case LEAST_WORK_LEFT:
                // copy the hosts list into a new list that uses MyHost instead of Host
                List<MyHost> sortedHostsLWL = new ArrayList<>();
                for (Host h : hosts) {
                    sortedHostsLWL.add((MyHost) h);
                }
                // get the host with the least work left, this time without considering the queue size
                MyHost minHostLWL = Collections.min(sortedHostsLWL, (o1, o2) -> {
                    // check only for the first decimal digit
                    int workLeftComparison = Integer.compare((int) (10 * o1.getWorkLeft()), (int) (10 * o2.getWorkLeft()));
                    if (workLeftComparison == 0) {
                        return Integer.compare((int) o1.getId(), (int) o2.getId());
                    }
                    return workLeftComparison;
                });
                // add the task to the host
                minHostLWL.addTask(task);
                break;
        }
    }
}
