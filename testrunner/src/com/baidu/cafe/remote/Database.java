/*
 * Copyright (C) 2011 Baidu.com Inc
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

package com.baidu.cafe.remote;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

/**
 * @author liyuping@baidu.com
 * @date 2011-6-22
 * @version
 * @todo
 */
public class Database {
    private ContentResolver mContentResolver;

    public Database(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * <p>
     * Query the given URI, returning a {@link Cursor} over the result set.
     * </p>
     * <p>
     * For best performance, the caller should follow these guidelines:
     * <ul>
     * <li>Provide an explicit projection, to prevent reading data from storage
     * that aren't going to be used.</li>
     * <li>Use question mark parameter markers such as 'phone=?' instead of
     * explicit values in the {@code selection} parameter, so that queries that
     * differ only by those values will be recognized as the same for caching
     * purposes.</li>
     * </ul>
     * </p>
     * 
     * @param uri
     *            The URI, using the content:// scheme, for the content to
     *            retrieve.
     * @param projection
     *            A list of which columns to return. Passing null will return
     *            all columns, which is inefficient.
     * @param selection
     *            A filter declaring which rows to return, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself). Passing null will
     *            return all rows for the given URI.
     * @param selectionArgs
     *            You may include ?s in selection, which will be replaced by the
     *            values from selectionArgs, in the order that they appear in
     *            the selection. The values will be bound as Strings.
     * @param sortOrder
     *            How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @return A Cursor object, which is positioned before the first entry, or
     *         null
     * @see Cursor
     */
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mContentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Inserts a row into a table at the given URL.
     * 
     * If the content provider supports transactions the insertion will be
     * atomic.
     * 
     * @param url
     *            The URL of the table to insert into.
     * @param values
     *            The initial values for the newly inserted row. The key is the
     *            column name for the field. Passing an empty ContentValues will
     *            create an empty row.
     * @return the URL of the newly created row.
     */
    public Uri insert(Uri url, ContentValues values) {
        return mContentResolver.insert(url, values);
    }

    /**
     * Update row(s) in a content URI.
     * 
     * If the content provider supports transactions the update will be atomic.
     * 
     * @param uri
     *            The URI to modify.
     * @param values
     *            The new field values. The key is the column name for the
     *            field. A null value will remove an existing field value.
     * @param where
     *            A filter to apply to rows before updating, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself).
     * @param selectionArgs
     *            You may include ?s in selection, which will be replaced by the
     *            values from selectionArgs, in the order that they appear in
     *            the selection. The values will be bound as Strings.
     * @return The number of rows updated.
     * @throws NullPointerException
     *             if uri or values are null
     */
    public int update(Uri url, ContentValues values, String where, String[] selectionArgs) {
        return mContentResolver.update(url, values, where, selectionArgs);
    }

    /**
     * Deletes row(s) specified by a content URI.
     * 
     * If the content provider supports transactions, the deletion will be
     * atomic.
     * 
     * @param url
     *            The URL of the row to delete.
     * @param where
     *            A filter to apply to rows before deleting, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself).
     * @param selectionArgs
     *            You may include ?s in selection, which will be replaced by the
     *            values from selectionArgs, in the order that they appear in
     *            the selection. The values will be bound as Strings.
     * @return The number of rows deleted.
     */
    public int delete(Uri url, String where, String[] selectionArgs) {
        return mContentResolver.delete(url, where, selectionArgs);
    }

    /**
     * Applies each of the {@link ContentProviderOperation} objects and returns
     * an array of their results. Passes through OperationApplicationException,
     * which may be thrown by the call to {@link ContentProviderOperation#apply}
     * . If all the applications succeed then a {@link ContentProviderResult}
     * array with the same number of elements as the operations will be
     * returned. It is implementation-specific how many, if any, operations will
     * have been successfully applied if a call to apply results in a
     * {@link OperationApplicationException}.
     * 
     * @param authority
     *            the authority of the ContentProvider to which this batch
     *            should be applied
     * @param operations
     *            the operations to apply
     * @return the results of the applications
     * @throws OperationApplicationException
     *             thrown if an application fails. See
     *             {@link ContentProviderOperation#apply} for more information.
     * @throws RemoteException
     *             thrown if a RemoteException is encountered while attempting
     *             to communicate with a remote provider.
     */
    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) {
        try {
            return mContentResolver.applyBatch(authority, operations);
        } catch (RemoteException re) {
            re.printStackTrace();
        } catch (OperationApplicationException oae) {
            oae.printStackTrace();
        }
        return null;
    }

}
