package com.zp4rker.ldb;

import com.zp4rker.ldb.db.SQLite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Table {

    private String name;
    private List<Column> columns = new ArrayList<>();
    private Column primaryKey;
    private SQLite sqLite;

    public Table(String name, Column primaryKey, Column... columns) {
        this.name = name;
        this.primaryKey = primaryKey;
        for (Column column : columns) {
            this.columns.add(column);
        }
    }

    public Table(String name, Collection<Column> columns, Column primaryKey) {
        this.name = name;
        this.primaryKey = primaryKey;
        this.columns.addAll(columns);
    }

    public Table(String name, Column... columns) {
        this.name = name;
        for (Column column : columns) {
            this.columns.add(column);
        }
        this.primaryKey = this.columns.get(0);
    }

    public Table(String name, Collection<Column> columns) {
        this.name = name;
        this.columns.addAll(columns);
        this.primaryKey = this.columns.get(0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public Column getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Column primaryKey) {
        this.primaryKey = primaryKey;
    }

    private static String getStringType(DataType dataType) {
        switch(dataType) {
            case STRING: return "VARCHAR";
            case INTEGER: return "INT";
            case FLOAT: return "FLOAT";
            default: return null;
        }
    }

    public void setSqLite(SQLite sqLite) {
        this.sqLite = sqLite;
    }

    public String getQuery() {
        String query = "CREATE TABLE IF NOT EXISTS " + getName() + " (";
        for (Column column : getColumns()) {
            query += "`" + column.name + "` ";
            query += Table.getStringType(column.dataType);
            query += (column.limit > 0 ? " (" + column.limit + "), " : ", ");
        }
        query += "PRIMARY KEY (`" + primaryKey.getName() + "`)";
        query += ");";
        return query;
    }
    
    public void insert(List<Column> columns) {
        if (getExact(columns.get(0)) == null) {
            String query = "INSERT INTO " + getName() + " (";
            for (Column column : columns) {
                if (columns.indexOf(column) < columns.size() - 1) {
                    query += "`" + column.getName() + "`, ";
                } else {
                    query += "`" + column.getName() + "`) ";
                }
            }
            query += "VALUES (";
            for (int i = 0; i < columns.size(); i++) {
                if (i < columns.size() - 1) {
                    query += "?, ";
                } else {
                    query += "?)";
                }
            }
            query += ";";
            try {
                PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
                for (int i = 0; i < columns.size(); i++) {
                    if (columns.get(i).dataType == DataType.STRING) {
                        s.setString(i + 1, columns.get(i).getValue().toString());
                    } else if (columns.get(i).dataType == DataType.INTEGER) {
                        s.setInt(i + 1, Integer.parseInt(columns.get(i).getValue().toString()));
                    } else {
                        s.setFloat(i + 1, Float.parseFloat(columns.get(i).getValue().toString()));
                    }
                }
                s.executeUpdate();
                s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("A row with that name already exists!");
        }
    }
    
    public List<Column> getExact(Column column) {
        List<Column> result = new ArrayList<>();
        String query = "SELECT * FROM " + getName() + " WHERE `" + column.getName() + "`=?";
        try {
            PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
            if (column.dataType == DataType.STRING) {
                s.setString(1, column.getValue().toString());
            } else if (column.dataType == DataType.INTEGER) {
                s.setInt(1, Integer.parseInt(column.getValue().toString()));
            } else {
                s.setFloat(1, Float.parseFloat(column.getValue().toString()));
            }
            ResultSet rs = s.executeQuery();
            try {
                for (int i = 0; i < getColumns().size(); i++) {
                    Column rCol = new Column(getColumns().get(i).getName(), getColumns().get(i).dataType,
                            getColumns().get(i).limit);
                    if (rCol.dataType == DataType.STRING) {
                        rCol.setValue(rs.getString(i + 1));
                    } else if (rCol.dataType == DataType.INTEGER) {
                        rCol.setValue(rs.getInt(i + 1));
                    } else {
                        rCol.setValue(rs.getFloat(i + 1));
                    }
                    result.add(rCol);
                }
                sqLite.close(s, rs);
            } catch (SQLException e) {
                s.close();
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public List<List<Column>> search(Column column) {
        List<List<Column>> results = new ArrayList<>();
        if (!column.getName().equalsIgnoreCase(primaryKey.getName())) {
            String query = "SELECT * FROM " + getName() + " WHERE `" + column.getName() + "`=?";
            try {
                PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
                if (column.dataType == DataType.STRING) {
                    s.setString(1, column.getValue().toString());
                } else if (column.dataType == DataType.INTEGER) {
                    s.setInt(1, Integer.parseInt(column.getValue().toString()));
                } else {
                    s.setFloat(1, Float.parseFloat(column.getValue().toString()));
                }
                ResultSet rs = s.executeQuery();
                while (rs.next()) {
                    List<Column> result = new ArrayList<>();
                    for (int i = 0; i < getColumns().size(); i++) {
                        Column rCol = new Column(getColumns().get(i).getName(), getColumns().get(i).dataType,
                                getColumns().get(i).limit);
                        if (getColumns().get(i).dataType == DataType.STRING) {
                            rCol.setValue(rs.getString(i + 1));
                        } else if (getColumns().get(i).dataType == DataType.INTEGER) {
                            rCol.setValue(rs.getInt(i + 1));
                        } else {
                            rCol.setValue(rs.getFloat(i + 1));
                        }
                        result.add(rCol);
                    }
                    results.add(result);
                }
                sqLite.close(s, rs);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return results;
        } else {
            return null;
        }
    }
    
    public List<List<Column>> getAll() {
        List<List<Column>> results = new ArrayList<>();
        String query = "SELECT * FROM " + getName();
        try {
            PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                List<Column> result = new ArrayList<>();
                for (int i = 0; i < getColumns().size(); i++) {
                    Column rCol = new Column(getColumns().get(i).getName(), getColumns().get(i).dataType,
                            getColumns().get(i).limit);
                    if (getColumns().get(i).dataType == DataType.STRING) {
                        s.setString(1, getColumns().get(i).getValue().toString());
                    } else if (getColumns().get(i).dataType == DataType.INTEGER) {
                        s.setInt(1, Integer.parseInt(getColumns().get(i).getValue().toString()));
                    } else {
                        s.setFloat(1, Float.parseFloat(getColumns().get(i).getValue().toString()));
                    }
                    result.add(rCol);
                }
                results.add(result);
            }
            sqLite.close(s, rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void delete(Column column) {
        if (column.getName().equalsIgnoreCase(primaryKey.getName())) {
            String query = "DELETE FROM " + getName() + " WHERE `" + column.getName() + "`=?";
            try {
                PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
                if (column.dataType == DataType.STRING) {
                    s.setString(1, column.getValue().toString());
                } else if (column.dataType == DataType.INTEGER) {
                    s.setInt(1, Integer.parseInt(column.getValue().toString()));
                } else {
                    s.setFloat(1, Float.parseFloat(column.getValue().toString()));
                }
                s.executeUpdate();
                s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Primary key must be used!");
        }
    }

    public void update(Column primaryKey, List<Column> columns) {
        if (!containsKey(columns)) {
            String query = "UPDATE " + getName() + " SET ";
            for (Column column : columns) {
                if (column.dataType == DataType.STRING) {
                    query += "`" + column.getName() + "`='" + column.getValue().toString() + "'";
                } else {
                    query += "`" + column.getName() + "`=" + column.getValue().toString();
                }
                if (columns.indexOf(column) == columns.size() - 1) {
                    query += " ";
                } else {
                    query += ", ";
                }
            }
            query += "WHERE `" + primaryKey.getName() + "`=";
            if (primaryKey.dataType == DataType.STRING) {
                query += "'" + primaryKey.getValue().toString() + "'";
            } else {
                query += primaryKey.getValue().toString();
            }
            try {
                PreparedStatement s = sqLite.getSQLConnection().prepareStatement(query);
                s.executeUpdate();
                s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            List<Column> newColumns = getExact(primaryKey);
            for (Column column : columns) {
                if (column.getName().equalsIgnoreCase(primaryKey.getName())) {
                    newColumns.set(0, column);
                } else {
                    newColumns.set(columns.indexOf(column), column);
                }
            }
            delete(primaryKey);
            insert(newColumns);
        }
    }

    private boolean containsKey(List<Column> columns) {
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(primaryKey.getName())) {
                return true;
            }
        }
        return false;
    }

}
