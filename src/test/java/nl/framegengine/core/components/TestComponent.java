package nl.framegengine.core.components;

/** Minimal concrete Component for use in tests. Must be top-level so that
 *  JsonHelper.objectToJson can reflectively instantiate it via getDeclaredConstructor(). */
public class TestComponent extends Component {}
