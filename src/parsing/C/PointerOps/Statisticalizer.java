package parsing.C.PointerOps;

public class Statisticalizer {
	public String identifier;
	public String type; //pointer or array
	public int opIncDec=0, opMul=0, opCall=0, opCast=0, opIncDecLoop=0, opMulLoop=0, opCallLoop=0, opCastLoop=0, opIncDecCondition=0, opMulCondition=0, opCallCondition=0, opCastCondition=0;
	public int maxLoopNum=0, maxConditionNum=0;
	public static String IDENTIFIER="identifier", TYPE="type", OPINCDEC="opincdec", OPMUL="opmul", OPINCDECLOOP="opincdecloop", OPMULLOOP="opmulloop", OPCALL="opcall", OPCAST="opcast", CONDITION="condition", LOOP="loop",
				  		 OPCALLLOOP="opcallloop", OPCASTLOOP="opcastloop", OPINCDECCONDITION="opincdeccondition", OPMULCONDITION="opmulcondition", OPCALLCONDITION="opcallcondition", OPCASTCONDITION="opcastcondition",
				  		 MAXLOOPNUM="maxloopnum", MAXCONDITIONNUM="maxconditionnum";
	public static String POINTERTYPE="pointer", ARRAYTYPE="array";
	public int getMaxLoopNum() {
		return maxLoopNum;
	}
	public void setMaxLoopNum(int maxLoopNum) {
		this.maxLoopNum = maxLoopNum;
	}
	public int getMaxConditionNum() {
		return maxConditionNum;
	}
	public void setMaxConditionNum(int maxConditionNum) {
		this.maxConditionNum = maxConditionNum;
	}
	public int total(){
		int totalNum=0;
		totalNum = opIncDec+opMul+opCall+opCast+opIncDecLoop+opMulLoop+opCallLoop+opCastLoop+opIncDecCondition+opMulCondition+opCallCondition+opCastCondition;
		return totalNum;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getType() {
		return type;
	}
	public int getOpCallLoop() {
		return opCallLoop;
	}
	public int getOpCastLoop() {
		return opCastLoop;
	}
	public int getOpIncDecCondition() {
		return opIncDecCondition;
	}
	public int getOpMulCondition() {
		return opMulCondition;
	}
	public int getOpCallCondition() {
		return opCallCondition;
	}
	public int getOpCastCondition() {
		return opCastCondition;
	}
	public void setOpCallLoop(int opCallLoop) {
		this.opCallLoop = opCallLoop;
	}
	public void setOpCastLoop(int opCastLoop) {
		this.opCastLoop = opCastLoop;
	}
	public void setOpIncDecCondition(int opIncDecCondition) {
		this.opIncDecCondition = opIncDecCondition;
	}
	public void setOpMulCondition(int opMulCondition) {
		this.opMulCondition = opMulCondition;
	}
	public void setOpCallCondition(int opCallCondition) {
		this.opCallCondition = opCallCondition;
	}
	public void setOpCastCondition(int opCastCondition) {
		this.opCastCondition = opCastCondition;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getOpIncDec() {
		return opIncDec;
	}
	public void setOpIncDec(int opIncDec) {
		this.opIncDec = opIncDec;
	}
	public int getOpMul() {
		return opMul;
	}
	public void setOpMul(int opMul) {
		this.opMul = opMul;
	}
	public int getOpIncDecLoop() {
		return opIncDecLoop;
	}
	public void setOpIncDecLoop(int opIncDecLoop) {
		this.opIncDecLoop = opIncDecLoop;
	}
	public int getOpMulLoop() {
		return opMulLoop;
	}
	public void setOpMulLoop(int opMulLoop) {
		this.opMulLoop = opMulLoop;
	}
	public int getOpCall() {
		return opCall;
	}
	public void setOpCall(int opCall) {
		this.opCall = opCall;
	}
	public int getOpCast() {
		return opCast;
	}
	public void setOpCast(int opCast) {
		this.opCast = opCast;
	}
}
