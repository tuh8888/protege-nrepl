package ucdenver.ccp.protege.nrepl;

import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;

import javax.annotation.Nonnull;
import java.util.List;

public class OWLOntologyChangeListenerImpl implements OWLOntologyChangeListener {
    @Override
    public void ontologiesChanged(@Nonnull List<? extends OWLOntologyChange> list) throws OWLException {

    }
}
