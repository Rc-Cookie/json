package com.github.rccookie.json.newer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.NotNull;

import static com.github.rccookie.json.newer.JsonUtils.validateValue;

public class JsonArrayList extends ArrayList<Object> {


    @Override
    public Object clone() {
        JsonArrayList clone = new JsonArrayList();
        clone.addAll(this);
        return clone;
    }

    @Override
    public Object set(int index, Object element) {
        return super.set(index, validateValue(element));
    }

    @Override
    public boolean add(Object o) {
        return super.add(validateValue(o));
    }

    @Override
    public void add(int index, Object element) {
        super.add(index, validateValue(element));
    }

    @Override
    public Object remove(int index) {
        return super.remove(index);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean remove(Object o) {
        return super.remove(o);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean addAll(Collection<?> c) {
        for(Object o : c) add(o);
        return !c.isEmpty();
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        int i = index;
        for(Object o : c) add(i++, o);
        return !c.isEmpty();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @NotNull
    @Override
    public ListIterator<Object> listIterator(int index) {
        return super.listIterator(index);
    }

    @NotNull
    @Override
    public ListIterator<Object> listIterator() {
        return super.listIterator();
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return super.iterator();
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public void forEach(Consumer<? super Object> action) {
        super.forEach(action);
    }

    @Override
    public Spliterator<Object> spliterator() {
        return super.spliterator();
    }

    @Override
    public boolean removeIf(Predicate<? super Object> filter) {
        return super.removeIf(filter);
    }

    @Override
    public void replaceAll(UnaryOperator<Object> operator) {
        super.replaceAll(o -> validateValue(operator.apply(o)));
    }

    @Override
    public void sort(Comparator<? super Object> c) {
        super.sort(c);
    }
}
