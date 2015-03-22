package com.wasteofplastic.askyblock.util;

public class Pair {
    private final int left;
    private final int right;

    public Pair(int left, int right) {
	this.left = left;
	this.right = right;
    }

    public int getLeft() {
	return left;
    }

    public int getRight() {
	return right;
    }

    @Override
    public boolean equals(Object o) {
	if (o == null)
	    return false;
	if (!(o instanceof Pair))
	    return false;
	Pair pairo = (Pair) o;
	return (this.left == pairo.getLeft()) && (this.right == pairo.getRight());
    }
}