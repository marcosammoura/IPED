/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.data;

public interface SelectionListener {

    void setSelected(IItemId item, boolean value);

    void clearAll();

    void selectAll();

}
