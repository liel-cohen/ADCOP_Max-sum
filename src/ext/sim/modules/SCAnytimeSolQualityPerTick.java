/**
 * 
 */
package ext.sim.modules;

import bgu.dcr.az.api.tools.Assignment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;

import ext.sim.tools.MyFileWriter;
import bgu.dcr.az.api.Agent;
import bgu.dcr.az.api.Hooks;
import bgu.dcr.az.api.exen.SystemClock;
import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.ano.Variable;
import bgu.dcr.az.api.exen.Execution;
import bgu.dcr.az.api.exen.Test;
import bgu.dcr.az.api.exen.stat.DBRecord;
import bgu.dcr.az.api.exen.stat.Database;
import bgu.dcr.az.api.exen.stat.VisualModel;
import bgu.dcr.az.api.exen.stat.vmod.LineVisualModel;
import bgu.dcr.az.exen.stat.AbstractStatisticCollector;


@Register(name = "asqpt-sc")
public class SCAnytimeSolQualityPerTick extends AbstractStatisticCollector<SCAnytimeSolQualityPerTick.ATSRecord> {

    @Variable(name = "sample-rate", description = "The sampling rate for solution quality", defaultValue = "1")
    private int samplingRate = 1;
    private int bestCost;
    private int ticksPerCycle = 1;
    MyFileWriter fileWriter;
    boolean writeToFile = true;

    public static class ATSRecord extends DBRecord {

        public final int currentSolutionQuality;
        public final long tickNum;
        public long cycles;
        public int execution;
        public int bestSolutionQuality;

        public ATSRecord(int bestSolutionQuality, long tickNum, int tpc, int execution, int currentSolutionQuality) {
            super();
            this.currentSolutionQuality = currentSolutionQuality;
            this.bestSolutionQuality = bestSolutionQuality;
            this.tickNum = tickNum;
            this.cycles = tickNum / tpc;
            this.execution = execution;
        }

        @Override
        public String provideTableName() {
            return "Anytime_Solution_Quality";
        }
    }

    @Override
    public VisualModel analyze(Database db, Test r) {
        LineVisualModel lvm = new LineVisualModel("Time", "Solution Quality", "Solution Quality Progress");
        try {
            ResultSet res = db.query("SELECT AVG (bestSolutionQuality) AS s, tickNum, ALGORITHM_INSTANCE FROM Anytime_Solution_Quality where TEST = '" + r.getName() + "' GROUP BY ALGORITHM_INSTANCE, tickNum ORDER BY tickNum");
            while (res.next()) {
                lvm.setPoint(res.getString("ALGORITHM_INSTANCE"), res.getLong("tickNum"), res.getDouble("s"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lvm;
    }

    @Override
    public void hookIn(final Agent[] a, final Execution e) {

        bestCost = Integer.MAX_VALUE;

        new Hooks.TickHook() {

            @Override
            public void hook(SystemClock clock) {
                if (clock.time() % samplingRate == 0) {
                    final Assignment assignment = e.getResult().getAssignment();
                    int cost = 0;
                    if (assignment != null) {
                        cost = assignment.calcCost(e.getGlobalProblem());
                    	if (cost < bestCost) {
                    		bestCost = cost;
                    	}
                    }
                     
                    if (writeToFile)
                    {
                        try 
                        {
    						//if (clock.time() >= 4850)
                        	//if (cost > 0)
                        	{
    							//System.out.println(cost);
                        		write(e.getTest().getCurrentExecutionNumber()+","+e.getTest().getCurrentExecutedAlgorithmInstanceName()+"," + clock.time() + "," + cost + "," + bestCost);
    						}
    															} catch (IOException e1) {
    																// TODO Auto-generated catch block
    															e1.printStackTrace();
    					}
                    }
 
                    submit(new ATSRecord(bestCost, clock.time(), ticksPerCycle, e.getTest().getCurrentExecutionNumber(), cost));
                }
            }

			private void write(String string) throws IOException 
			{
			    if (fileWriter == null)
			    {
			    	fileWriter = MyFileWriter.getInstance("ExecutionNumber,Algorithm,Tick,Cost,AnytimeCost");
			    }
			    	
			    fileWriter.write(string);
			        
			}
        }.hookInto(e);

        new Hooks.ReportHook("ticksPerCycle") {

            @Override
            public void hook(Agent a, Object[] args) {
                ticksPerCycle = (Integer) args[0];
            }
        }.hookInto(e);
    }
    

    @Override
    public String getName() {
        return "Anytime Solution Quality Per Ticks";
    }
}