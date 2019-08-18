package ext.sim.modules;

import bgu.dcr.az.api.ano.Register;
import bgu.dcr.az.api.exen.Execution;
import bgu.dcr.az.api.exen.ExecutionResult;
import bgu.dcr.az.api.prob.Problem;
import bgu.dcr.az.api.tools.Assignment;
import bgu.dcr.az.exen.correctness.AbstractCorrectnessTester;

import java.util.*;

@Register(name="WAC2001star-tester")
public class WAC2001starTester extends AbstractCorrectnessTester {

    @Override
	public CorrectnessTestResult test(Execution exec, ExecutionResult result) {
        Assignment ass = result.getAssignment();
        final Problem globalProblem = exec.getGlobalProblem();
        
        //TEST THE RESULT AND RETURN TESTED RESULT OBJECT THAT REPRESENT THE RESOULT CORRECTNESS
        WAC2001star solver = new WAC2001star(new CentralizedProblem(globalProblem));
        int[] solution = solver.findOpt();
        Assignment sol = new Assignment();
        for (int i=0; i<globalProblem.getNumberOfVariables(); i++){
        	sol.assign(i, solution[i]);
        }
        if (ass.calcCost(globalProblem) != sol.calcCost(globalProblem)){
        	return new CorrectnessTestResult(sol, false); //<-- FAIL
        }	else {
        	return new CorrectnessTestResult(null, true); //<-- PASS
        }
  	}
    
    public class WAC2001star extends WAC3star {

    	final int NO_SUPPORT = -1;
    	int [][][] supportAC;		//the value in [i][a][j] is the support of value 'a' from variable 'i' in variable 'j'
    	int [] supportNC;
    	
    	public WAC2001star(CentralizedProblem p) {
    		super(p);
    		int n = p.numOfVariables();
    		int d = p.getDomainSize();
    		supportAC = new int[n][d][n];
    		supportNC = new int[n];
    		for (int i=0; i<n; i++){
    			for (int a=0; a<d; a++){
    				for (int j=0; j<n; j++){
    					supportAC[i][a][j] = NO_SUPPORT;
    				}
    			}
    		}
    	}
    	
    	@Override
    	protected boolean findSupport(int varInitiatingMac, int i, int j){
    		boolean supported = true;
    		for (Integer a: currentDomain[i]){
    			if (supportAC[i][a][j]==NO_SUPPORT || !currentDomain[j].contains(supportAC[i][a][j])){
    				int min = Integer.MAX_VALUE;
    				int v = NO_SUPPORT;
    				for (Integer b: currentDomain[j]){
    					if (min==0) break;
    					int check = check(i, a.intValue(), j, b.intValue()); 
    					if (check < min) {
    						min = check;
    						v = b.intValue();
    					}
    				} 
    				supportAC[i][a][j] = v;
    				if (min!=0 && min<Integer.MAX_VALUE){
    					if (varInitiatingMac>=0){
    						supportInfo[varInitiatingMac].push(new FindSupportInfo(i,j,min,a));
    					}
    					problem.addToValueCost(i, a, min);
    					for (int b=0; b<problem.getDomainSize(); b++){
    						problem.addToConflictCost(i, a, j, b, - min);
    					}
    					if (supportNC[i]==a.intValue()){
    						supported = false;
    					}
    				}
    			}
    		}
    		if (!supported){
    			int min = Integer.MAX_VALUE;
    			Integer v = null;
    			for (Integer a: currentDomain[i]){
    				if (problem.valueCost(i, a)<min){
    					min = problem.valueCost(i, a);
    					v = a;
    				}
    			}
    			supportNC[i] = v.intValue();
    			if (min>0) {
    				if (varInitiatingMac>=0){
    					varCostChanged[varInitiatingMac].push(new VarCostChanged(i,min));
    				}
    				globalCost += min;
    				for (int a=0; a<problem.getDomainSize(); a++){
    					problem.addToValueCost(i, a, - min);
    				}
    				return true;
    			}
    			
    		}
    		return false;
    	}
    	
    	protected void undoFindSupport(int varInitiatingMac){
    		while (!supportInfo[varInitiatingMac].isEmpty()){
    			FindSupportInfo info = supportInfo[varInitiatingMac].pop();
    			
    			problem.addToValueCost(info.i, info.iVal, - info.cost);
    			supportAC[info.i][info.iVal][info.j] = NO_SUPPORT;
    			for (int b=0; b<problem.getDomainSize(); b++){
    				problem.addToConflictCost(info.i, info.iVal, info.j, b, info.cost);
    			}
    		}
    		while (!varCostChanged[varInitiatingMac].isEmpty()){
    			VarCostChanged temp = varCostChanged[varInitiatingMac].pop();
    			globalCost -= temp.costChange;
    			for (int a=0; a<problem.getDomainSize(); a++){
    				problem.addToValueCost(temp.var, a, temp.costChange);
    			}
    		}
    	}
    	
    	protected boolean mnc(int varInitiatingMac){
    		boolean emptyDomain = false;
    		for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    			int min = Integer.MAX_VALUE;
    			for (Integer a:currentDomain[i]){
    				if (problem.valueCost(i, a) < min) {
    					min = problem.valueCost(i, a); 
    					supportNC[i] = a;
    				}
    			}
    			if (min>0 && min<Integer.MAX_VALUE){
    				globalCost += min;
    				mncCostChange[varInitiatingMac].push(new VarCostChanged(i,min));
    				for (int a=0; a<problem.getDomainSize(); a++){
    					problem.addToValueCost(i, a, -min);
    				}
    			}
    		}
    		for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    			Iterator<Integer> iter = currentDomain[i].iterator();
    			while (iter.hasNext()){
    				Integer a = iter.next();
    				if (globalCost + tempCost[varInitiatingMac] + problem.valueCost(i, a) >= ub){
    					iter.remove();
    					mncReductionsVars[varInitiatingMac].push(i);
    					mncReductionsVals[varInitiatingMac].push(a);
    					mncReductions[i].push(a);
    					if (currentDomain[i].isEmpty()){
    						emptyDomain = true;
    					}
    				}
    			}
    		}
    		return emptyDomain;
    	}
    }
    
    public class WAC3star extends BB {

    	Stack<Integer>[] prunedVars;	//stacks of variables that the MAC function performed in variable 'index' removed values from their domain
    	Stack<Integer>[] prunedVals; 	//stacks of values removed from the domain of the var (by the MAC performed in variable 'index') in the same position in the stack
    	Stack<Integer>[] reductions;	//stack of values removed from the domain of 'index' by pruneVar
    	Stack<FindSupportInfo>[] supportInfo;
    	Stack<VarCostChanged>[] varCostChanged;
    	
    	//MNC
    	Stack<Integer>[] mncReductionsVars;
    	Stack<Integer>[] mncReductionsVals;
    	Stack<Integer>[] mncReductions;
    	Stack<VarCostChanged>[] mncCostChange;
    	
    	int globalCost;
    	
    	@SuppressWarnings("unchecked")
    	public WAC3star(CentralizedProblem p){
    		super(p);
    		globalCost =0;
    		prunedVars = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		prunedVals = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		reductions = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		supportInfo = (Stack<FindSupportInfo>[])new Stack[problem.numOfVariables()];
    		varCostChanged = (Stack<VarCostChanged>[])new Stack[problem.numOfVariables()];
    		
    		mncReductionsVars = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		mncReductionsVals = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		mncReductions = (Stack<Integer>[])new Stack[problem.numOfVariables()];
    		mncCostChange = (Stack<VarCostChanged>[])new Stack[problem.numOfVariables()];
    		
    		for (int i=0; i<problem.numOfVariables(); i++){
    			prunedVars[i] = new Stack<Integer>();
    			prunedVals[i] = new Stack<Integer>();
    			reductions[i] = new Stack<Integer>();
    			supportInfo[i] = new Stack<FindSupportInfo>();
    			varCostChanged[i] = new Stack<VarCostChanged>();
    			
    			mncReductionsVars[i] = new Stack<Integer>();
    			mncReductionsVals[i] = new Stack<Integer>();
    			mncReductions[i] = new Stack<Integer>();
    			mncCostChange[i] = new Stack<VarCostChanged>();
    		}
    	}
    	
    	protected class VarCostChanged{
    		int var;
    		int costChange;
    		
    		public VarCostChanged(int v, int c){
    			var = v;
    			costChange = c;
    		}
    	}
    	
    	protected class FindSupportInfo{
    		int i;
    		int j;
    		int cost;
    		int iVal;
    		
    		FindSupportInfo(int i, int j, int cost, int iVal){
    			this.i = i;
    			this.j = j;
    			this.cost = cost;
    			this.iVal = iVal;
    		}
    	}

    	public int[] findOpt(){
    		tempAssign = new int[problem.numOfVariables()];
    		for (int i=0; i<tempAssign.length; i++){
    			assign(i,NO_ASSIGNMENT);
    		}
    		mac(-1);
    		int i = 0;
    		while (i>=0){
//    			if (System.currentTimeMillis() - time> 200000) return null;
    			if (i==problem.numOfVariables()){		//reached a leaf (full assignment)
    				updateBestAssign();
    				i--;
    			}
    			
    			if (currentDomain[i].isEmpty()){
    				if (tempAssign[i]!=NO_ASSIGNMENT){
    					undoMac(i);
    					undoMnc(i);
    					undoLookAhead(i);
    				}
    				assign(i,NO_ASSIGNMENT);
    				tempCost[i]=0;
    				updateDomain(i);
    				i--;
    			}
    			else {									//the current domain is not empty - try the next value
    				if (tempAssign[i]!=NO_ASSIGNMENT){
    					undoMac(i);
    					undoMnc(i);
    					undoLookAhead(i);
    				}
    				Integer val = currentDomain[i].first();
    				currentDomain[i].remove(val);
    				int stepCost = valueCost(i, val);
    				int temp = (i-1>=0 ? tempCost[i-1] :0);
    				if (globalCost + temp + stepCost < ub){
    					assign(i,val);
    					tempCost[i] = stepCost + temp;
    					lookAhead(i);
    					if (!mnc(i) & !mac(i)){
    						i++;
    					}
    				} else{
    					assign(i,NO_ASSIGNMENT);
    				}
    			}
    		}
    		time = System.currentTimeMillis() - time;
    		return bestAssignment;
    	}
    	
    	protected boolean mac(int varInitiatingMac){
    		boolean emptyDomain = false;
    		HashSet<Integer> Q = new HashSet<Integer>(problem.numOfVariables());
    		for (int k=varInitiatingMac+1; k<problem.numOfVariables(); k++){
    			Q.add(k);
    		}
    		Iterator<Integer> iter = Q.iterator();
    		int j;
    		while (!Q.isEmpty()){
    			j = iter.next().intValue();
    			iter.remove();
    			boolean flag = false;
    			for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    				flag = findSupport(varInitiatingMac, i, j) || flag;
    				if (pruneVar(varInitiatingMac, i)){
    					Q.add(i);
    					iter = Q.iterator();
    					if (currentDomain[i].isEmpty()){
    						emptyDomain = true;
    					}
    				}
    			}
    			if (flag){
    				for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    					if (pruneVar(varInitiatingMac, i)){
    						Q.add(i);
    						iter = Q.iterator();
    						if (currentDomain[i].isEmpty()){
    							emptyDomain = true;
    						}
    					}
    				}
    			}
    		}
    		return emptyDomain;
    	}
    	
    	protected void undoMac(int varInitiatingMac){
    		undoPruneVar(varInitiatingMac);
    		undoFindSupport(varInitiatingMac);
    	}
    	
    	protected boolean mnc(int varInitiatingMac){
    		boolean emptyDomain = false;
    		for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    			int min = Integer.MAX_VALUE;
    			for (Integer a:currentDomain[i]){
    				if (problem.valueCost(i, a) < min) {
    					min = problem.valueCost(i, a); 
    				}
    			}
    			if (min>0 && min<Integer.MAX_VALUE){
    				globalCost += min;
    				mncCostChange[varInitiatingMac].push(new VarCostChanged(i,min));
    				for (int a=0; a<problem.getDomainSize(); a++){
    					problem.addToValueCost(i, a, -min);
    				}
    			}
    		}
    		for (int i=varInitiatingMac+1; i<problem.numOfVariables(); i++){
    			Iterator<Integer> iter = currentDomain[i].iterator();
    			while (iter.hasNext()){
    				Integer a = iter.next();
    				if (globalCost + tempCost[varInitiatingMac] + problem.valueCost(i, a) >= ub){
    					iter.remove();
    					mncReductionsVars[varInitiatingMac].push(i);
    					mncReductionsVals[varInitiatingMac].push(a);
    					mncReductions[i].push(a);
    					if (currentDomain[i].isEmpty()){
    						emptyDomain = true;
    					}
    				}
    			}
    		}
    		return emptyDomain;
    	}
    	
    	protected void undoMnc(int varInitiatingMac){
    		while (!mncCostChange[varInitiatingMac].isEmpty()){
    			VarCostChanged v = mncCostChange[varInitiatingMac].pop();
    			globalCost -= v.costChange;
    			for (int a=0; a<problem.getDomainSize(); a++){
    				problem.addToValueCost(v.var, a, v.costChange);
    			}
    		}
    		while (!mncReductionsVars[varInitiatingMac].isEmpty()){
    			Integer var = mncReductionsVars[varInitiatingMac].pop();
    			Integer val = mncReductionsVals[varInitiatingMac].pop();
    			mncReductions[var].pop();
    			currentDomain[var].add(val);
    		}
    	}
    	
    	protected boolean pruneVar(int varInitiatingMac, int i){
    		boolean change = false;
    		Iterator<Integer> iter = currentDomain[i].iterator();
    		Integer val;
    		while (iter.hasNext()){
    			val = iter.next();
    			if (tempCost[i] + globalCost + problem.valueCost(i, val.intValue()) >= ub){
    				if (varInitiatingMac>=0){
    					prunedVars[varInitiatingMac].push(i);
    					prunedVals[varInitiatingMac].push(val);
    				}
    				reductions[i].push(val);
    				iter.remove();
    				change = true;
    			}
    		}
    		return change;
    	}

    	protected void undoPruneVar(int varInitiatingMac){
    		while (!prunedVars[varInitiatingMac].isEmpty()){
    			Integer var = prunedVars[varInitiatingMac].pop();
    			Integer val = prunedVals[varInitiatingMac].pop();
    			reductions[var].pop();
    			currentDomain[var].add(val);
    		}
    	}
    	
    	protected boolean findSupport(int varInitiatingMac, int i, int j){
    		for (Integer a: currentDomain[i]){
    			int min = Integer.MAX_VALUE;
    			for (Integer b: currentDomain[j]){
    				int check = check(i, a.intValue(), j, b.intValue()); 
    				if (check < min) {
    					min = check;
    				}
    			}
    			if (min>0 && min<Integer.MAX_VALUE){
    				if (varInitiatingMac>=0){
    					supportInfo[varInitiatingMac].push(new FindSupportInfo(i,j,min,a));
    				}
    				problem.addToValueCost(i, a, min);
    				for (int b=0; b<problem.getDomainSize(); b++){
    					problem.addToConflictCost(i, a, j, b, - min);
    				}
    			}
    		}
    		int min = Integer.MAX_VALUE;
    		for (Integer a: currentDomain[i]){
    			if (problem.valueCost(i, a)<min){
    				min = problem.valueCost(i, a);
    			}
    		}
    		if (min>0){
    			if (varInitiatingMac>=0){
    				varCostChanged[varInitiatingMac].push(new VarCostChanged(i,min));
    			}
    			globalCost += min;
    			for (int a=0; a<problem.getDomainSize(); a++){
    				problem.addToValueCost(i, a, -min);
    			}
    			return true;
    		}
    		return false;
    	}
    	
    	protected void undoFindSupport(int varInitiatingMac){
    		while (!supportInfo[varInitiatingMac].isEmpty()){
    			FindSupportInfo info = supportInfo[varInitiatingMac].pop();
    			
    			problem.addToValueCost(info.i, info.iVal, - info.cost);
    			for (int b=0; b<problem.getDomainSize(); b++){
    				problem.addToConflictCost(info.i, info.iVal, info.j, b, info.cost);
    			}
    		}
    		while (!varCostChanged[varInitiatingMac].isEmpty()){
    			VarCostChanged temp = varCostChanged[varInitiatingMac].pop();
    			globalCost -= temp.costChange;
    			for (int a=0; a<problem.getDomainSize(); a++){
    				problem.addToValueCost(temp.var, a, temp.costChange);
    			}
    		}
    	}
    	
    	protected void updateDomain(int i){
    		currentDomainEqualsDomain(i);
    		for (Integer val: reductions[i]){
    			currentDomain[i].remove(val);
    		}
    		for (Integer val: mncReductions[i]){
    			currentDomain[i].remove(val);
    		}
    	}

    	protected void updateBestAssign(){
    		ub = globalCost + tempCost[problem.numOfVariables()-1];
    		for (int j=0; j<problem.numOfVariables(); j++){
    			bestAssignment[j] = tempAssign[j];
    		}
    	}
    }

    public class BB {
    	int ub;
    	int[] bestAssignment;
    	CentralizedProblem problem;
    	int[] tempAssign;
    	SortedSet<Integer>[] currentDomain;
    	int[] tempCost;
    	
    	long cc;
    	int numAssignment;
    	public long time;
    	
    	final int NO_ASSIGNMENT = -1;
    	
    	@SuppressWarnings("unchecked")
    	public BB(CentralizedProblem p){
    		time = System.currentTimeMillis();
    		problem = p;
    		ub = Integer.MAX_VALUE;
    		bestAssignment = new int[problem.numOfVariables()];
    		currentDomain = (SortedSet<Integer>[])new SortedSet[problem.numOfVariables()];
    		for (int i=0; i<bestAssignment.length; i++){
    			bestAssignment[i] = NO_ASSIGNMENT;
    			currentDomainEqualsDomain(i);
    		}
    		tempCost = new int[problem.numOfVariables()];
    		
    		cc=0;
    		numAssignment=0;
    	}
    	
    	public int[] findOpt(){
    		tempAssign = new int[problem.numOfVariables()];
    		for (int i=0; i<tempAssign.length; i++){
    			assign(i,NO_ASSIGNMENT);
    		}
    		
    		int i = 0;
    		while (i>=0){
//    			if (System.currentTimeMillis() - time> 200000) return null;
    			if (i==problem.numOfVariables()){		//reached a leaf (full assignment)
    				updateBestAssign();
    				i--;
    			}
    			
    			if (currentDomain[i].isEmpty()){
    				if (tempAssign[i]!=NO_ASSIGNMENT){
    					undoLookAhead(i);
    				}
    				assign(i,NO_ASSIGNMENT);
    				tempCost[i]=0;
    				currentDomainEqualsDomain(i);
    				i--;
    			}
    			else {									//the current domain is not empty - try the next value
    				if (tempAssign[i]!=NO_ASSIGNMENT){
    					undoLookAhead(i);
    				}
    				Integer val = currentDomain[i].first();
    				currentDomain[i].remove(val);
    				//Integer val = currentDomain[i].remove(0);
    				int stepCost = valueCost(i, val);
    				int temp = (i-1>=0 ? tempCost[i-1] :0);
    				if (temp+stepCost < ub){
    					assign(i,val);
    					tempCost[i] = stepCost + temp;
    					lookAhead(i);
    					i++;
    				} else{
    					assign(i,NO_ASSIGNMENT);
    				}
    			}
    		}
    		time = System.currentTimeMillis() - time;
    		return bestAssignment;
    	}
    	
    	protected void updateBestAssign(){
    		ub = tempCost[problem.numOfVariables()-1];
    		for (int j=0; j<problem.numOfVariables(); j++){
    			bestAssignment[j] = tempAssign[j];
    		}
    	}
    	
    	protected void currentDomainEqualsDomain(int variable){
    		currentDomain[variable] = new TreeSet<Integer>();
    		for (int i=0; i<problem.getDomainSize(); i++){
    			currentDomain[variable].add(new Integer(i));
    		}
    	}
    	
    	protected void lookAhead(int i){
    		//System.out.println("looking ahead from "+i);
    		for (int j=i+1; j<problem.numOfVariables(); j++){
    			for (Integer k: currentDomain[j]){
    				//System.out.println("adding cost var "+j+" val "+ k + " cost "+check(i, tempAssign[i], j, k));
    				problem.addToValueCost(j, k.intValue(), check(i, tempAssign[i], j, k));
    			}
    		}
    	}
    	
    	protected void undoLookAhead(int i){
    		//System.out.println("undo looking ahead from "+i);
    		for (int j=i+1; j<problem.numOfVariables(); j++){
    			for (Integer k: currentDomain[j]){
    				//System.out.println("removing cost var "+j+" val "+ k + " cost "+check(i, tempAssign[i], j, k));
    				problem.addToValueCost(j, k.intValue(), -check(i, tempAssign[i], j, k));
    			}
    		}
    	}
    	
    	public void assign(int var, int val){
    		if (val!=NO_ASSIGNMENT) numAssignment++;
    		tempAssign[var]=val;
    	}
    	
    	public int check(int var1, int var2){
    		cc++;
    		return problem.check(var1, tempAssign[var1], var2, tempAssign[var2]);
    	}
    	
    	public int check(int var1, int val1, int var2, int val2){
    		cc++;
    		return problem.check(var1, val1, var2, val2);
    	}
    	
    	public int valueCost(int var, int val){
    		return problem.valueCost(var,val);
    	}

    	public void print(){
    		System.out.println(this);
    	}
    	
    	@Override
    	public String toString(){
    		String str = this.getClass().toString();
    		str += " - cost: "+ub+"; Assignment: ";
    		for (int i=0; i<bestAssignment.length; i++){
    			str += "<" + i + "," + bestAssignment[i] + ">,";
    		}
    		str += "CCs:" + cc +", Assignments:" + numAssignment;
    		return str;
    	}
    	
    	public int getNumOfAssignments(){
    		return numAssignment;
    	}
    	public long getCCs(){
    		return cc;
    	}
    	public long getTime(){
    		return time;
    	}

    }

    public class CentralizedProblem {

    	private int domain;
    	private int[][][][] constrains;		//[var1][val1][var2][val2] = cost
    	private int[][] valuesCost;			//first index is the variable, second index is the value

    	public CentralizedProblem(Problem problem){
    		int d = problem.getDomainSize(0);
    		this.domain = d;
    		int n = problem.getNumberOfVariables();
    		valuesCost = new int[n][d];
    		
    		constrains = new int[n][d][n][d];
    		for (int i=0; i<n; i++){
    			for (int vali=0; vali<d; vali++){
    				for (int j=0; j<n; j++){
    					for (int valj=0; valj<d; valj++){
    						if (problem.type().isAsymmetric()){
    							constrains[i][vali][j][valj] = problem.getConstraintCost(i, i, vali, j, valj);
    							constrains[i][vali][j][valj] += problem.getConstraintCost(j, i, vali, j, valj);
    						} else {
    							constrains[i][vali][j][valj] = problem.getConstraintCost(i, vali, j, valj);
    						}
    					}
    				}
    			}
    		}
    	}
    	
    	public CentralizedProblem(int varNum, int domain){		//no price for variables. conflicts needed to be added later
    		this.domain = domain;
    		constrains = new int[varNum][domain][varNum][domain];
    		valuesCost = new int[varNum][domain];
    	}
    	
    	public CentralizedProblem(CentralizedProblem other){
    		int d = other.getDomainSize();
    		this.domain = d;
    		int n = other.numOfVariables();
    		valuesCost = new int[n][d];
    		
    		constrains = new int[n][d][n][d];
    		for (int i=0; i<n; i++){
    			for (int vali=0; vali<d; vali++){
    				for (int j=0; j<n; j++){
    					for (int valj=0; valj<d; valj++){
    						constrains[i][vali][j][valj] = other.constrains[i][vali][j][valj];
    					}
    				}
    			}
    		}
    	}

    	public CentralizedProblem(int n, int d, double p1, double p2, int maxCost) {
    		this.domain = d;
    		valuesCost = new int[n][d];
    		constrains = new int[n][d][n][d];

    		Random rand = new Random();
    		for (int i=0; i<n-1; i++){
    			for (int j=i+1; j<n; j++){
    				if (rand.nextDouble() < p1){		//make constrains between i and j
    					for (int iVal=0; iVal<domain; iVal++){
    						for (int jVal=0; jVal<domain; jVal++){
    							if (rand.nextDouble() < p2){		//make a conflict between value iVal in i and jVal in j
    								int cost = rand.nextInt(maxCost+1);
    								constrains[i][iVal][j][jVal] = cost;
    								constrains[j][jVal][i][iVal] = cost;
    							}
    						}
    					}
    				}
    			}
    		}
    	}

    	public int check(int var1, int val1, int var2, int val2){
    		return constrains[var1][val1][var2][val2];
    	}
    	
    	public int valueCost(int var, int val){
    		return valuesCost[var][val];
    	}

    	public void addToValueCost(int var, int val, int costChange){
    		valuesCost[var][val] += costChange;
    	}
    	
    	public void addToConflictCost(int var1, int val1, int var2, int val2, int costChange){
    		constrains[var1][val1][var2][val2] += costChange;
    		constrains[var2][val2][var1][val1] += costChange;	//to maintain symetrical constrains
    	}
    	
    	public int numOfVariables(){
    		return valuesCost.length;
    	}

    	public int getDomainSize(){
    		return domain;
    	}

    	public void addConflict(int var1, int val1, int var2, int val2, int cost){
    		constrains[var1][val2][var2][val2] = cost;
    		constrains[var2][val2][var1][val1] = cost;
    	}
    	
     	public void print(){
    		System.out.println(this.toString());
    	}

    	@Override
    	public String toString(){
    		String str = "";
    		for (int leftVar=0; leftVar<numOfVariables(); leftVar++){
    			str += "var "+leftVar+" values cost:\n";
    			for (int leftVal=0; leftVal<getDomainSize(); leftVal++){
    				str += leftVal+"("+valueCost(leftVar, leftVal)+") ";
    			}
    			str += "\n\nconstrains:\n  ";
    			for (int topVar=0; topVar<numOfVariables(); topVar++){
    				str += "  " + topVar;
    				for (int d=0; d<getDomainSize()-2; d++) str += " ";
    			}
    			str += "\n";
    			str += leftVar + " |";
    			for (int topVar=0; topVar<numOfVariables(); topVar++){
    				for (int topVal=0; topVal<getDomainSize(); topVal++){
    					str += topVal;
    				}
    				str += "|";
    			}
    			for (int leftVal=0; leftVal<getDomainSize(); leftVal++){
    				
    				str += "\n "+leftVal+"|";
    				for (int topVar=0; topVar<numOfVariables(); topVar++){
    					for (int topVal=0; topVal<getDomainSize(); topVal++){
    						str += constrains[topVar][topVal][leftVar][leftVal];
    					}
    					str += "|";
    				}
    			}
    			str += "\n\n";
    		}

    		return str+"----------------------------------------------------------------";

    	}

    }

}

