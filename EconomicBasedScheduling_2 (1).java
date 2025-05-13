package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import java.text.DecimalFormat;
import java.util.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import java.util.*;

public class EconomicBasedScheduling_2 {
    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUser, calendar, traceFlag);

            // Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            // Create VMs
            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                vmList.add(new Vm(i, brokerId, 1000, 1, 2048, 10000, 100000,"Xen", new CloudletSchedulerTimeShared()));
            }
            broker.submitVmList(vmList);

            // Create Cloudlets (Tasks) with cost-based SJF scheduling
            List<Cloudlet> cloudletList = new ArrayList<>();
            double[] economicCost = new double[5];

            for (int i = 0; i < 5; i++) {
                int length = (int) (1000 + Math.random() * 4000); // Random task length
                economicCost[i] = 0.05 + (Math.random() * 0.1); // Randomized cost to avoid zero-division

                Cloudlet cloudlet = new Cloudlet(i, length, 1, 300, 300,new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                cloudlet.setUserId(brokerId);
                cloudlet.setVmId(-1); // Ensure VM assignment is dynamic
                cloudletList.add(cloudlet);
            }

            // Sort Cloudlets based on Economic-SJF (lower execution cost per unit length first)
            cloudletList.sort(Comparator.comparingDouble(c -> economicCost[c.getCloudletId()] / c.getCloudletLength()));

            // Assign sorted cloudlets to VMs in a round-robin manner
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVmId(vmList.get(i % vmList.size()).getId());
            }

            // Print Cloudlet Execution Order
            System.out.println("Cloudlet Execution Order (Economic-SJF):");
            for (Cloudlet cloudlet : cloudletList) {
                System.out.println("Cloudlet " + cloudlet.getCloudletId() +
                        " - Length: " + cloudlet.getCloudletLength() +
                        ", Economic Cost: " + economicCost[cloudlet.getCloudletId()] +
                        ", Assigned VM: " + cloudlet.getVmId());
            }

            broker.submitCloudletList(cloudletList);

            // Start Simulation
            CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Print Results
            System.out.println("\n========== OUTPUT ==========");
            System.out.println("Cloudlet ID\tSTATUS\tData center ID\tVM ID\tTime\tStart Time\tFinish Time");
            for (Cloudlet cloudlet : newList) {
                System.out.println(cloudlet.getCloudletId() + "\tSUCCESS\t" +
                        cloudlet.getResourceId() + "\t" +
                        cloudlet.getVmId() + "\t" +
                        cloudlet.getActualCPUTime() + "\t" +
                        cloudlet.getExecStartTime() + "\t" +
                        cloudlet.getFinishTime());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) {
        try {
            List<Host> hostList = new ArrayList<>();
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(1000))); // Processing Element (CPU)
            
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
}

