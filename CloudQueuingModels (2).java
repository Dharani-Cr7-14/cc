package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

public class CloudQueuingModels {
    public static void main(String[] args) {
        try {
            for (int schedulingMethod = 1; schedulingMethod <= 3; schedulingMethod++) {
                System.out.println("\nRunning Simulation for Scheduling Method: " + schedulingMethod);

                // Initialize CloudSim for each run
                int numUser = 1;
                Calendar calendar = Calendar.getInstance();
                boolean traceFlag = false;
                CloudSim.init(numUser, calendar, traceFlag);

                // Create Datacenter
                Datacenter datacenter = createDatacenter("Datacenter_0");

                // Create Broker for each run
                DatacenterBroker broker = new DatacenterBroker("Broker_" + schedulingMethod);
                int brokerId = broker.getId();

                // Create Virtual Machines (VMs)
                List<Vm> vmList = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    vmList.add(new Vm(i, brokerId, 1000, 1, 2048, 10000, 100000,
                            "Xen", new CloudletSchedulerTimeShared()));
                }
                broker.submitVmList(vmList);

                // Create Cloudlets (Tasks)
                List<Cloudlet> cloudletList = new ArrayList<>();
                Random random = new Random();

                for (int i = 0; i < 5; i++) {
                    int length = 1000 + random.nextInt(4000);  // Random length between 1000 - 5000
                    Cloudlet cloudlet = new Cloudlet(i, length, 1, 300, 300,
                            new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                    cloudlet.setUserId(brokerId);
                    cloudletList.add(cloudlet);
                }

                // Run Simulation
                runSimulation(cloudletList, vmList, broker, schedulingMethod);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSimulation(List<Cloudlet> cloudletList, List<Vm> vmList, DatacenterBroker broker, int schedulingMethod) {
        switch (schedulingMethod) {
            case 1:
                System.out.println("Applying First-Come, First-Served (FCFS)");
                break;
            case 2:
                System.out.println("Applying Shortest Job First (SJF)");
                cloudletList.sort(Comparator.comparingLong(Cloudlet::getCloudletLength));
                break;
            case 3:
                System.out.println("Applying Round Robin Scheduling");
                roundRobinScheduling(cloudletList, vmList);
                break;
        }

        if (schedulingMethod != 3) {
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVmId(vmList.get(i % vmList.size()).getId());
            }
        }

        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        printResults(newList, schedulingMethod);
    }

    private static Datacenter createDatacenter(String name) {
        try {
            List<Host> hostList = new ArrayList<>();
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));

            hostList.add(new Host(0, new RamProvisionerSimple(4096), new BwProvisionerSimple(10000),
                    100000, peList, new VmSchedulerTimeShared(peList)));

            String arch = "x86";
            String os = "Linux";
            String vmm = "Xen";
            double timeZone = 10.0;
            double costPerSec = 3.0;
            double costPerMem = 0.05;
            double costPerStorage = 0.001;
            double costPerBw = 0.02;

            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                    arch, os, vmm, hostList, timeZone, costPerSec, costPerMem, costPerStorage, costPerBw);

            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList),
                    new ArrayList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void roundRobinScheduling(List<Cloudlet> cloudlets, List<Vm> vms) {
        int vmIndex = 0;
        for (Cloudlet cloudlet : cloudlets) {
            cloudlet.setVmId(vms.get(vmIndex).getId());
            vmIndex = (vmIndex + 1) % vms.size();
        }
    }

    private static void printResults(List<Cloudlet> cloudlets, int schedulingMethod) {
        System.out.println("\n========== OUTPUT FOR SCHEDULING METHOD " + schedulingMethod + " ==========");
        System.out.printf("%-12s %-8s %-15s %-6s %-8s %-12s %-12s%n",
                "Cloudlet ID", "STATUS", "Data center ID", "VM ID", "Time", "Start Time", "Finish Time");

        for (Cloudlet cloudlet : cloudlets) {
            System.out.printf("%-12d %-8s %-15d %-6d %-8.2f %-12.2f %-12.2f%n",
                    cloudlet.getCloudletId(), "SUCCESS",
                    cloudlet.getResourceId(), cloudlet.getVmId(),
                    cloudlet.getActualCPUTime(),
                    cloudlet.getExecStartTime(), cloudlet.getFinishTime());
        }
    }
}
