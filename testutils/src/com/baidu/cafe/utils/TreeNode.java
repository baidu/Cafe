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

package com.baidu.cafe.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/*
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
*/

/**
 * a muti-node tree is not thread-safe
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-5-13
 * @version
 * @todo
 */
public class TreeNode<T> {
    public static String           vertical       = null;
    public static String           horizontal     = null;
    public static String           big_horizontal = null;
    public static String           vertical_T     = null;
    public static String           horizontal_T   = null;
    public static String           left           = null;

    private T                      data           = null;
    private int                    index          = -1;
    private TreeNode<T>            parent         = null;
    private ArrayList<TreeNode<T>> children       = null;

    // load string to avoid to compile unicode 
    static {
//        try {
//            Document doc = new SAXReader().read(new FileInputStream(new File("res/strings.xml")));
//            Element rootElement = doc.getRootElement();
//            List<Element> strings = rootElement.elements("string");
//            vertical = strings.get(0).getStringValue();
//            horizontal = strings.get(1).getStringValue();
//            big_horizontal = strings.get(2).getStringValue();
//            vertical_T = strings.get(3).getStringValue();
//            horizontal_T = strings.get(4).getStringValue();
//            left = strings.get(5).getStringValue();
//        } catch (FileNotFoundException e) {
            vertical = "|";
            horizontal = "-";
            big_horizontal = "--";
            vertical_T = "T";
            horizontal_T = "|-";
            left = "'-";
            //e.printStackTrace();
//        } catch (DocumentException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public TreeNode(T data) {
        this.data = data;
        this.children = new ArrayList<TreeNode<T>>();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public ArrayList<TreeNode<T>> getBrothers() {
        ArrayList<TreeNode<T>> brothers = new ArrayList<TreeNode<T>>();
        if (parent != null) {
            for (TreeNode<T> child : parent.getChildren()) {
                if (!child.equals(this)) {
                    brothers.add(child);
                }
            }
        }

        return brothers;
    }

    public ArrayList<TreeNode<T>> getChildren() {
        return children;
    }

    public void addChild(TreeNode<T> child) {
        child.index = children.size();
        children.add(child);
        child.parent = this;
    }

    public void addChildren(ArrayList<TreeNode<T>> children) {
        for (TreeNode<T> child : children) {
            addChild(child);
        }
    }

    public void removeChild(TreeNode<T> node) {
        int childrenSize = children.size();
        for (int i = 0; i < childrenSize; i++) {
            if (children.get(i) == node) {
                children.remove(i);
            }
        }
    }

    private void insertFrontWhitespace(TreeNode<T> root, TreeNode<T> node, StringBuffer sb) {
        int number = 2 + node.parent.data.toString().length();
        for (int i = 0; i < number; i++) {
            sb.append(" ");
        }
        if (node.parent == root) {
            return;
        }
        sb.append(vertical);
        insertFrontWhitespace(root, node.parent, sb);
    }

    private void drawFamily(StringBuffer sb, TreeNode<T> root) {
        int childrenSize = children.size();

        // draw itself
        sb.append(horizontal + data);
        if (childrenSize == 0) {
            sb.append("\n");
            return;
        }

        // draw the first child
        if (childrenSize == 1) {
            sb.append(big_horizontal);
        } else {
            sb.append(vertical_T);
        }
        children.get(0).drawFamily(sb, root);

        // draw middle children
        int i = 1;
        for (; i < childrenSize - 1; i++) {
            StringBuffer t = new StringBuffer();
            insertFrontWhitespace(root, children.get(i), t);
            sb.append(t.reverse());
            sb.append(horizontal_T);
            children.get(i).drawFamily(sb, root);
        }

        // draw the last child
        if (childrenSize > 1) {
            StringBuffer t = new StringBuffer();
            insertFrontWhitespace(root, children.get(i), t);
            sb.append(t.reverse());
            sb.append(left);
            children.get(i).drawFamily(sb, root);
        }
    }

    public String drawTree() {
        StringBuffer sb = new StringBuffer(1024 * 10);
        drawFamily(sb, this);
        return sb.toString();
    }

    public interface NodeCallBack<T> {
        void doSomething(TreeNode<T> node, int depth);

        boolean shouldRepeat(TreeNode<T> node);

        void doWhenCompleted(TreeNode<T> node, int depth);

        boolean shouldStop(TreeNode<T> node);
    }

    /**
     * travel the tree with preorder and do something at every tree node
     * 
     * @param callBack
     * @param maxDepth
     *            the depth of root is 1
     * @return should continue
     */
    public boolean preorderTraversal(NodeCallBack<T> callBack, int maxDepth) {
        if (callBack.shouldStop(this)) {
            return false;
        }

        callBack.doSomething(this, maxDepth);

        if (maxDepth > 0) {
            for (int i = 0; i < children.size(); i++) {
                TreeNode<T> child = children.get(i);
                if (!child.preorderTraversal(callBack, maxDepth - 1)) {
                    return false;
                }
                if (callBack.shouldRepeat(this)) {
                    //Logger.println("Repeat: " + this.data.toString());
                    callBack.doSomething(this, maxDepth);
                }
            }
        }

        callBack.doWhenCompleted(this, maxDepth);
        return true;
    }

    public void breadthFirstTraversal(NodeCallBack<T> callBack, int maxDepth) {
        Queue<TreeNode<T>> queue = new LinkedList<TreeNode<T>>();
        queue.offer(this);
        while (queue.size() != 0) {
            TreeNode<T> head = queue.poll();
            if (countDistance(head, this) > maxDepth - 1) {
                break;
            }
            callBack.doSomething(head, maxDepth);
            for (int i = 0; i < head.children.size(); i++) {
                queue.offer(head.children.get(i));
            }
            //            printQueue(queue);
        }
    }

    private int countDistance(final TreeNode<T> child, TreeNode<T> ancestor) {
        int distance = 0;
        TreeNode<T> node = child;
        while (node != ancestor) {
            node = node.getParent();
            distance++;
        }
        return distance;
    }

    private void printQueue(Queue<TreeNode<T>> queue) {
        //        System.out.println("******************* print queue *********************");
        for (TreeNode<T> node : queue) {
            System.out.println(node);
        }
    }

    public static void main(String[] args) {
        //      Map map = new HashMap();
        //      Iterator iter = map.entrySet().iterator();
        //      while (iter.hasNext()) {
        //          Entry entry = (Entry) iter.next();
        //          Object key = entry.getKey();
        //          Object val = entry.getValue();
        //      }

        TreeNode<String> root = new TreeNode<String>("0");
        TreeNode<String> node1 = new TreeNode<String>("1");
        TreeNode<String> node2 = new TreeNode<String>("22");
        TreeNode<String> node3 = new TreeNode<String>("333");
        TreeNode<String> node4 = new TreeNode<String>("4444");
        TreeNode<String> node5 = new TreeNode<String>("55555");
        TreeNode<String> node6 = new TreeNode<String>("666666");
        TreeNode<String> node7 = new TreeNode<String>("7777777");
        TreeNode<String> node8 = new TreeNode<String>("88888888");
        TreeNode<String> node9 = new TreeNode<String>("999999999");
        root.addChild(node1);
        root.addChild(node2);
        root.addChild(node7);
        node1.addChild(node3);
        node1.addChild(node4);
        node2.addChild(node5);
        node2.addChild(node6);
        node4.addChild(node8);

        // test draw tree
        System.out.println(root.drawTree());
        System.out.println(node1.drawTree());
        System.out.println(node4.drawTree());
        System.out.println(node7.drawTree());

        System.out.println("test remove child");
        node1.removeChild(node4);
        System.out.println(root.drawTree());

        System.out.println("test preorderTraversal");
        node1.addChild(node4);
        node8.addChild(node9);
        System.out.println(root.drawTree());
        root.preorderTraversal(new NodeCallBack<String>() {

            @Override
            public void doSomething(TreeNode<String> node, int maxDepth) {
                System.out.print(node.getData() + "(" + (3 - maxDepth) + "), ");
            }

            @Override
            public void doWhenCompleted(TreeNode<String> node, int maxDepth) {

            }

            @Override
            public boolean shouldRepeat(TreeNode<String> node) {
                return false;
            }

            @Override
            public boolean shouldStop(TreeNode<String> node) {
                return "55555".equals(node.getData());
            }
        }, 3);
        System.out.println("");

        System.out.println("test getBrothers");
        ArrayList<TreeNode<String>> brothers = node1.getBrothers();
        for (TreeNode<String> brother : brothers) {
            System.out.print(brother.getData() + ", ");
        }
        System.out.println("");

        System.out.println("test breadthFirstTraversal");
        System.out.println(root.drawTree());
        root.breadthFirstTraversal(new NodeCallBack<String>() {

            @Override
            public boolean shouldRepeat(TreeNode<String> node) {
                return false;
            }

            @Override
            public void doSomething(TreeNode<String> node, int maxDepth) {
                System.out.print(node.getData() + ", ");
            }

            @Override
            public void doWhenCompleted(TreeNode<String> node, int depth) {
            }

            @Override
            public boolean shouldStop(TreeNode<String> node) {
                return false;
            }
        }, 2);
        System.out.println("");
    }

}
