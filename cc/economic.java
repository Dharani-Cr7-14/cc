package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

public class EconomicBasedScheduling_2 {
    public static void main(String[] args) {
        try {
            System.out.println("\nRunning Simulation for Economic-Based SJF Scheduling");

            // Step 1: Initialize CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // Step 2: Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // Step 3: Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker_Eco");
            int brokerId = broker.getId();

            // Step 4: Create VMs
            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Vm vm = new Vm(i, brokerId, 1000, 1, 2048, 10000, 100000,
                               "Xen", new CloudletSchedulerTimeShared());
                vmList.add(vm);
            }
            broker.submitVmList(vmList);

            // Step 5: Create Cloudlets and assign random cost
            List<Cloudlet> cloudletList = new ArrayList<>();
            Map<Integer, Double> costMap = new HashMap<>();
            Random rand = new Random();

            for (int i = 0; i < 5; i++) {
                int length = 1000 + rand.nextInt(4000);
                double cost = 0.05 + rand.nextDouble() * 0.1;

                Cloudlet cloudlet = new Cloudlet(i, length, 1, 300, 300,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
                costMap.put(i, cost);
            }

            // Step 6: Sort cloudlets by (cost / length) - Economic-SJF
            cloudletList.sort(Comparator.comparingDouble(
                    c -> costMap.get(c.getCloudletId()) / c.getCloudletLength()
            ));

            // Step 7: Assign VMs in round-robin
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVmId(vmList.get(i % vmList.size()).getId());
            }

            // Step 8: Submit Cloudlets and Run Simulation
            broker.submitCloudletList(cloudletList);
            CloudSim.startSimulation();
            List<Cloudlet> resultList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Step 9: Print Results
            printResults(resultList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to create Datacenter
    private static Datacenter createDatacenter(String name) {
        try {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000)));

            List<Host> hostList = new ArrayList<>();
            hostList.add(new Host(0,
                    new RamProvisionerSimple(4096),
                    new BwProvisionerSimple(10000),
                    100000, peList,
                    new VmSchedulerTimeShared(peList)));

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

    // Method to print Cloudlet Results
    private static void printResults(List<Cloudlet> list) {
        System.out.println("\n=== Results for Economic-Based SJF ===");
        System.out.printf("%-12s %-8s %-15s %-6s %-8s %-12s %-12s%n",
                "Cloudlet ID", "STATUS", "Datacenter ID", "VM ID", "Time", "Start Time", "Finish Time");

        for (Cloudlet c : list) {
            System.out.printf("%-12d %-8s %-15d %-6d %-8.2f %-12.2f %-12.2f%n",
                    c.getCloudletId(), "SUCCESS", c.getResourceId(), c.getVmId(),
                    c.getActualCPUTime(), c.getExecStartTime(), c.getFinishTime());
        }
    }
}