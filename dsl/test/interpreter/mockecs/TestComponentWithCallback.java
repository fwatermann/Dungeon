package interpreter.mockecs;

import semanticanalysis.types.DSLCallback;
import semanticanalysis.types.DSLContextMember;
import semanticanalysis.types.DSLType;

import java.util.function.Consumer;

@DSLType
public class TestComponentWithCallback extends Component {
    private Entity entity;

    public Entity getEntity() {
        return entity;
    }

    @DSLCallback private Consumer<Entity> onInteraction;

    public TestComponentWithCallback(@DSLContextMember(name = "entity") Entity entity) {
        super(entity);
        this.entity = entity;
    }
}