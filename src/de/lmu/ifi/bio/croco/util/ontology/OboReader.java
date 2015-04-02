package de.lmu.ifi.bio.croco.util.ontology;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.croco.util.FileUtil;


public class OboReader {
    public enum OboField
    {
        //Term fields
        ID("id"),
        NAME("name"),
        SYNONYM("synonym"),
        IS_A("is_a"),
        relationship("relationship")
        ;
        String name;
        OboField(String name)
        {
            this.name=name;
        }

     
        public String getName()
        {
            return name;
        }
        
    }
    
    public class OboElement
    {
        List<OboElement> parents = new ArrayList<OboElement>();
        List<OboElement> children = new ArrayList<OboElement>();
        
        String id = null;
        String name = null;
     
        List<String> synonym = new ArrayList<String>();
        
        @Override
        public String toString()
        {
            return name;
        }
        public OboElement getRoot()
        {
            Stack<OboElement> stack = new Stack<OboElement>();
            stack.add(this);
            while(!stack.isEmpty())
            {
                OboElement top = stack.pop();
                if ( top.parents.size() == 0){
                    return top;
                }
                for(OboElement parent : top.parents)
                {
                    stack.add(parent);
                }
            }
            return null;
        }
        
        public List<OboElement> getAllChildren() {
            Set<OboElement> ret = new HashSet<OboElement>();
            
            Stack<OboElement> stack = new Stack<OboElement>();
            stack.add(this);
            
            while(!stack.isEmpty())
            {
                OboElement top = stack.pop();
                
                ret.add(top);
                
                if ( top.children != null)
                {
                    for(OboElement child : top.children)
                    {
                        stack.add(child);
                    }
                }
            }
            return new ArrayList<OboElement>(ret);
        }

        public List<OboElement> getParents() {
            return parents;
        }

        public List<OboElement> getChildren() {
            return children;
        }

        public List<String> getSynonyms() {
            return synonym;
        }
        public String getId() {
            return id;
        }
        
    }
    Pattern TERM = Pattern.compile("\\[(\\w+)\\]");
    Pattern SEP = Pattern.compile(":");
    Pattern SYNONYM = Pattern.compile("\"([^\"]+)\"");
    
    HashMap<String,OboElement> elements = new HashMap<String,OboElement>();
    
    public Collection<OboElement> getElements() {
        return elements.values();
    }
    public OboElement getElement(String id)
    {
        return elements.get(id);
    }
    public OboReader(File file) throws Exception
    {
        Iterator<String> it = FileUtil.getLineIterator(file);
        
        OboElement current = null;
        
        HashMap<String,String> relations = new HashMap<String,String>();
        
        String type = null;
        while(it.hasNext())
        {
            String line = it.next();
            
            if ( line.trim().length() ==0)
                continue;
            
            java.util.regex.Matcher matcher = TERM.matcher(line);
            if (matcher.matches())
            {
                type = matcher.group(1);
                if ( current != null)
                {
                    
                    elements.put(current.id,current);
                    
                }
                
                current = new OboElement();
                continue;
            }
            
            if ( type != null && type.equals("Term"))
            {
                String[] tokens = SEP.split(line,2);
                String key = tokens[0];
                String value = tokens[1].trim();
                if ( OboField.ID.name.equals(key))
                {
                    current.id = value;
                }else if ( OboField.NAME.name.equals(key))
                {
                    current.name = value;
                }else if ( OboField.IS_A.name.equals(key))
                {
                    relations.put(current.id, value.split("!")[0].trim());
                }else if ( OboField.SYNONYM.name.equals(key))
                {
                    matcher = SYNONYM.matcher(line);
                    if ( matcher.find())
                    {
                        current.synonym.add(matcher.group(1));
                        
                    }else
                    {
                        throw new RuntimeException("Incorret line:" + line);
                    }
                }else if ( OboField.relationship.name.equals(key))
                {
                    relations.put(current.id, value.split(" ")[1].trim());
                }
            }
        
        }
        elements.put(current.id,current);
       
        for(OboElement element : elements.values())
        {
            if ( relations.containsKey(element.id))
            {
                OboElement parent = elements.get(relations.get(element.id));
                //String parentId = is_a_relations.get(element.id);
                parent.children.add(element);
                element.parents.add(parent);
            }
        }
        
        
    }
 
    
  
}
