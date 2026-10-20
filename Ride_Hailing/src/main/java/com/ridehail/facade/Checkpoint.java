package com.ridehail.facade;

/** Exit after a domain commit, before the journal update, to demonstrate recovery. */
public enum Checkpoint {
  NONE,
  A,
  B,
  C
}
