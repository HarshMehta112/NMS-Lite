package Database;

import java.beans.PropertyEditorSupport;
import java.sql.*;
import java.util.*;


public class Operations
{

    private Connection connection;

    // Constructor
    public Operations(Connection connection)
    {

        this.connection = connection;
    }


    //insert operation
    // remove whereclause
    //try catch in every method
    public int insert (String tableName, Map< String, Object > data, String whereClause)
    {

        ArrayList< String > columnNames = new ArrayList<>(data.keySet());

        String columns = String.join(",", columnNames);

        String values = String.join(",", Collections.nCopies(data.size(), "?"));

        String query = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            int index = 1;

            for ( Object value : columnNames )
            {
                statement.setObject(index++, data.get(value));
            }

            return statement.executeUpdate();

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return 0;
    }


    // Update operation
    public int update (String tableName, Map< String, Object > data, String whereClause)
    {

        ArrayList< String > columnNames = new ArrayList<>(data.keySet());

        String setClause = "";

        for ( int index = 0; index < columnNames.size(); index++ )
        {
            setClause += columnNames.get(index) + "= ?";

            if ( index != columnNames.size() - 1 )
            {
                setClause += ",";
            }
        }

        String query = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            int indexs = 1;

            for ( String column : columnNames )
            {
                statement.setObject(indexs++, data.get(column));
            }

            return statement.executeUpdate();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return 0;
    }


    // Delete operation
    public int delete (String tableName, String whereClause)
    {

        String query = "DELETE FROM " + tableName + " WHERE " + whereClause;

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            return statement.executeUpdate();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return 0;
    }

    // Select operation
    public List< Map< String, Object > > selectwithWhere (String tableName, ArrayList< String > columnNames,String whereClause)
    {

        String columns = String.join(",", columnNames);

        String query = "SELECT " + columns + " FROM " + tableName + whereClause;

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            ResultSet resultSet = statement.executeQuery();

            List< Map< String, Object > > resultList = new ArrayList<>();

            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while ( resultSet.next() )
            {
                Map< String, Object > row = new HashMap<>();

                for ( int iterator = 1; iterator <= columnCount; iterator++ )
                {
                    row.put(metaData.getColumnName(iterator), resultSet.getObject(iterator));
                }
                resultList.add(row);
            }
            return resultList;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return null;
    }

    public List< Map< String, Object > > selectQuery (String query) throws SQLException
    {

        try ( PreparedStatement statement = connection.prepareStatement(query) )
        {
            ResultSet resultSet = statement.executeQuery();

            List< Map< String, Object > > resultList = new ArrayList<>();

            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while ( resultSet.next() )
            {
                Map< String, Object > row = new HashMap<>();

                for ( int iterator = 1; iterator <= columnCount; iterator++ )
                {
                    row.put(metaData.getColumnName(iterator), resultSet.getObject(iterator));
                }
                resultList.add(row);
            }
            return resultList;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return null;
    }


}
