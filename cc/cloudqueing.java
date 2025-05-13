package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

public class cloudqueuing {

    public static void main(String[] args) {
        try {
            for (int schedulingMethod = 1; schedulingMethod <= 3; schedulingMethod++) {
                System.out.println("\nRunning Simulation for Scheduling Method: " + schedulingMethod);

                // Step 1: Initialize CloudSim
                CloudSim.init(1, Calendar.getInstance(), false);

                // Step 2: Create Datacenter
                Datacenter datacenter = createDatacenter("Datacenter_0");

                // Step 3: Create Broker
                DatacenterBroker broker = new DatacenterBroker("Broker_" + schedulingMethod);
                int brokerId = broker.getId();

                // Step 4: Create VMs
                List<Vm> vmList = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    vmList.add(new Vm(i, brokerId, 1000, 1, 2048, 10000, 100000,
                            "Xen", new CloudletSchedulerTimeShared()));
                }
                broker.submitVmList(vmList);

                // Step 5: Create Cloudlets
                List<Cloudlet> cloudletList = new ArrayList<>();
                Random random = new Random();
                for (int i = 0; i < 5; i++) {
                    int length = 1000 + random.nextInt(4000);
                    Cloudlet cloudlet = new Cloudlet(i, length, 1, 300, 300,
                            new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                    cloudlet.setUserId(brokerId);
                    cloudletList.add(cloudlet);
                }

                // Step 6: Run simulation for selected method
                runSimulation(cloudletList, vmList, broker, schedulingMethod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSimulation(List<Cloudlet> cloudletList, List<Vm> vmList,
                                      DatacenterBroker broker, int method) {

        if (method == 1) {
            System.out.println("Applying First-Come, First-Served (FCFS)");
        } else if (method == 2) {
            System.out.println("Applying Shortest Job First (SJF)");
            cloudletList.sort(Comparator.comparingLong(Cloudlet::getCloudletLength));
        } else if (method == 3) {
            System.out.println("Applying Round Robin Scheduling");
            roundRobinScheduling(cloudletList, vmList);
        }

        // For FCFS and SJF: assign VMs in round-robin
        if (method != 3) {
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVmId(vmList.get(i % vmList.size()).getId());
            }
        }

        broker.submitCloudletList(cloudletList);

        // Start and stop simulation
        CloudSim.startSimulation();
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        // Print result
        printResults(newList, method);
    }

    private static Datacenter createDatacenter(String name) {
        try {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));

            List<Host> hostList = new ArrayList<>();
            hostList.add(new Host(0,
                    new RamProvisionerSimple(4096),
                    new BwProvisionerSimple(10000),
                    100000, peList, new VmSchedulerTimeShared(peList)));

            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                    "x86", "Linux", "Xen", hostList,
                    10.0, 3.0, 0.05, 0.001, 0.02);

            return new Datacenter(name, characteristics,
                    new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void roundRobinScheduling(List<Cloudlet> cloudlets, List<Vm> vms) {
        int index = 0;
        for (Cloudlet c : cloudlets) {
            c.setVmId(vms.get(index % vms.size()).getId());
            index++;
        }
    }

    private static void printResults(List<Cloudlet> cloudlets, int method) {
        System.out.println("\n=== Results for Scheduling Method " + method + " ===");
        System.out.printf("%-12s %-8s %-15s %-6s %-8s %-12s %-12s%n",
                "Cloudlet ID", "STATUS", "Data center ID", "VM ID", "Time", "Start Time", "Finish Time");

        for (Cloudlet c : cloudlets) {
            System.out.printf("%-12d %-8s %-15d %-6d %-8.2f %-12.2f %-12.2f%n",
                    c.getCloudletId(), "SUCCESS", c.getResourceId(), c.getVmId(),
                    c.getActualCPUTime(), c.getExecStartTime(), c.getFinishTime());
        }
    }
}