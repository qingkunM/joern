package parsing.C.PointerOps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Alias {
	List<Set<String>> aliasList = new ArrayList<Set<String>>();
	public void addAlias(String key, String value){
		if(aliasList.size()==0){
			Set<String> set = new HashSet<String>();
			set.add(key);
			set.add(value);
			aliasList.add(set);
			return;
		}
		for(int i=0; i<aliasList.size(); i++){
			if(aliasList.get(i).contains(key)){
				aliasList.get(i).add(value);
				return;
			}else if(aliasList.get(i).contains(value)){
				aliasList.get(i).add(key);
				return;
			}
		}
		Set<String> tmp = new HashSet<String>();
		tmp.add(key);
		tmp.add(value);
		aliasList.add(tmp);
	}
	public String findAlias(String id, Map<String, Statisticalizer> map){
		for(Map.Entry<String, Statisticalizer> entry : map.entrySet()){
			String str = entry.getKey();
			for(int i=0; i<aliasList.size(); i++){
				if(aliasList.get(i).contains(str) && aliasList.get(i).contains(id)){
					return str;
				}
			}
		}
		return id;
	}
}
