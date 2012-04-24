/**
 * Copyright 2011 Miklos Vajna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bpm.console.client.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement(name="nodeInstance")
public class NodeInstanceRef
{
  private Long id;
  private Long type;
  private String nodeId;
  private String nodeName;
  private Date date;

  public NodeInstanceRef(long id, long type, String nodeId, String nodeName, Date date)
  {

	  if(null==date)
		  throw new IllegalArgumentException("An instance requires a date");

	  this.id = id;
	  this.type = type;
	  this.nodeId = nodeId;
	  this.nodeName = nodeName;
	  this.date = date;
  }

  @XmlElement(name = "id")
  public long getId()
  {
	  return id;
  }

  public void setId(long id)
  {
	  this.id = id;
  }

  @XmlElement(name = "type")
  public long getType()
  {
	  return type;
  }

  public void setType(long type)
  {
	  this.type = type;
  }

  @XmlElement(name = "nodeId")
  public String getNodeId()
  {
	  return nodeId;
  }

  public void setNodeId(String nodeId)
  {
	  this.nodeId = nodeId;
  }

  @XmlElement(name = "nodeName")
  public String getNodeName()
  {
	  return nodeName;
  }

  public void setNodeName(String nodeName)
  {
	  this.nodeName = nodeName;
  }

  @XmlElement(name = "date")
  public Date getDate()
  {
	  return date;
  }

  public void setDate(Date date)
  {
	  this.date = date;
  }

  public boolean equals(Object o)
  {
	  if (this == o) return true;
	  if (o == null || getClass() != o.getClass()) return false;

	  NodeInstanceRef that = (NodeInstanceRef) o;

	  if (id != null ? !id.equals(that.id) : that.id != null) return false;

	  return true;
  }

  public int hashCode()
  {
	  int result;
	  result = (id != null ? id.hashCode() : 0);
	  return result;
  }
}
