package tools.callGraph;

import java.io.Serializable;

public class PathNode implements Cloneable, Serializable {
	public String code;
	public String type;//Function or Invocation
	public String location;
	
}
