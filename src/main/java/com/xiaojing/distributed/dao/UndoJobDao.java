package com.xiaojing.distributed.dao;

import com.google.inject.Inject;

import com.xiaojing.distributed.model.UndoJob;
import com.xiaojing.distributed.util.DataBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;


/**
 * Created by xiaojing on 15/10/14.
 */
public class UndoJobDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoJobDao.class);
    @Inject
    private DbUtil dbUtil;

    public int insertUndo(Connection connection, UndoJob undoJob) throws SQLException {
        String sql = "insert into " + getTableName() + " values(:id,:distribute_job_id,:state,:content)";
        Map<String, Object> params = new HashMap<>();
        params.put("id", undoJob.getId());
        params.put("distribute_job_id", undoJob.getDistributeJobId());
        params.put("state", undoJob.getState());
        params.put("content", undoJob.getContent());
        return dbUtil.insert(connection, sql, params);
    }

    //todo shared database?
    public List<UndoJob> getSuspendedByDistributeJobId(String distributeJobId) throws SQLException {
        Map<String, DataSource> map = DataBaseUtil.dataSourceMap;
        Set<String> keySet = map.keySet();
        Set<DataSource> dataSourceSet = new HashSet<DataSource>();
        for (String key : keySet) {
            dataSourceSet.add(map.get(key));
        }

        Connection connection = null;

        String sql;
        Map<String, Object> params = new HashMap<>();
        List<UndoJob> list = new LinkedList<>();

        for (DataSource dataSource : dataSourceSet) {
            try {
                connection = dataSource.getConnection();
                sql = "select id, distribute_job_id, state, content from " + getTableName() + " where distribute_job_id=:distributeJobId";
                params.put("distributeJobId", distributeJobId);
                List<UndoJob> undoJobs = dbUtil.queryList(connection, sql, UndoJob.class, params);
                list.addAll(undoJobs);
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return list;
    }

    public int updateUndoStateById(Connection connection, String undoId, String state) throws SQLException {
        String sql = "update " + getTableName() + " set state=:state where id=:id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", undoId);
        params.put("state", state);
        return dbUtil.update(connection, sql, params);
    }


    public UndoJob getAndLockUndoById(Connection connection, String id) throws SQLException {
        String sql = "select id, distribute_job_id, state, content from " + getTableName() + " where id=:id for update";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        return dbUtil.queryOne(connection, sql, UndoJob.class, params);
    }

    private String getTableName() {
        return "undo_job";
    }
}
