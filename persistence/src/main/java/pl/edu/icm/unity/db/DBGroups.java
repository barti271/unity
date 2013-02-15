/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.exceptions.GroupAlreadyExistsException;
import pl.edu.icm.unity.exceptions.GroupNotKnownException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;


/**
 * Groups related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBGroups
{
	private GroupResolver groupResolver;
	private DBLimits limits;
	private JsonSerializer<Group> jsonS;
	
	@Autowired
	public DBGroups(GroupResolver groupResolver, SerializersRegistry reg, DB db)
	{
		this.groupResolver = groupResolver;
		this.limits = db.getDBLimits();
		jsonS = reg.getSerializer(Group.class);
	}
	
	/**
	 * Adds a new group. Pass null parent to create top-level ROOT group.
	 * @param parent
	 * @param name
	 * @throws InternalException
	 * @throws GroupNotKnownException
	 * @throws ElementAlreadyExistsException
	 */
	public void addGroup(Group toAdd, SqlSession sqlMap) 
		throws InternalException, GroupNotKnownException, 
		GroupAlreadyExistsException
	{
		if (toAdd.getName().length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Group name length must not exceed " + 
					limits.getNameLimit() + " characters");
			
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean pb = groupResolver.resolveGroup(toAdd.getParentPath(), mapper);

		GroupBean param = new GroupBean();
		param.setName(toAdd.getName());
		param.setParent(pb.getId());
		param.setContents(jsonS.toJson(toAdd));
		if (param.getContents().length > limits.getContentsLimit())
			throw new IllegalGroupValueException("Group metadata size (description, rules, ...) is too big.");
		try
		{
			mapper.insertGroup(param);
			sqlMap.clearCache();
		} catch (PersistenceException e)
		{
			throw new GroupAlreadyExistsException(toAdd.toString(), e);  
		}
	}
	
	public GroupContents getContents(String path, int filter, SqlSession sqlMap) 
			throws InternalException, GroupNotKnownException
	{
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean gb = groupResolver.resolveGroup(path, mapper);

		GroupContents ret = new GroupContents();
		try
		{
			if ((filter & GroupContents.GROUPS) != 0)
			{
				List<GroupBean> subGroupsRaw = mapper.getSubgroups(gb.getId());
				ret.setSubGroups(convertGroups(subGroupsRaw, mapper));
			}
			if ((filter & GroupContents.LINKED_GROUPS) != 0)
			{
				List<GroupBean> linkedGroupsRaw = mapper.getLinkedGroups(gb.getId());
				ret.setLinkedGroups(convertGroups(linkedGroupsRaw, mapper));
			}
			if ((filter & GroupContents.MEMBERS) != 0)
			{
				//TODO
			}
			if ((filter & GroupContents.METADATA) != 0)
			{
				//TODO
			}
		} catch (PersistenceException e)
		{
			throw new InternalException("Can't retrieve contents of the " + path + " group", e);
		}
		return ret;
	}
	
	private List<Group> convertGroups(List<GroupBean> src, GroupsMapper mapper)
	{
		List<Group> ret = new ArrayList<Group>(src.size());
		for (int i=0; i<src.size(); i++)
			ret.add(groupResolver.resolveGroupBean(src.get(i), mapper));
		return ret;
	}

}
