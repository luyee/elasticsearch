/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.lucene.search;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BitSet;
import org.elasticsearch.common.lucene.docset.DocIdSets;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 */
public class TermsFilterTests extends ElasticsearchTestCase {

    @Test
    public void testTermFilter() throws Exception {
        String fieldName = "field1";
        Directory rd = new RAMDirectory();
        IndexWriter w = new IndexWriter(rd, new IndexWriterConfig(new KeywordAnalyzer()));
        for (int i = 0; i < 100; i++) {
            Document doc = new Document();
            int term = i * 10; //terms are units of 10;
            doc.add(new Field(fieldName, "" + term, StringField.TYPE_NOT_STORED));
            doc.add(new Field("all", "xxx", StringField.TYPE_NOT_STORED));
            w.addDocument(doc);
            if ((i % 40) == 0) {
                w.commit();
            }
        }
        LeafReader reader = SlowCompositeReaderWrapper.wrap(DirectoryReader.open(w, true));
        w.close();

        TermFilter tf = new TermFilter(new Term(fieldName, "19"));
        DocIdSet dis = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        assertTrue(dis == null || dis.iterator() == null);

        tf = new TermFilter(new Term(fieldName, "20"));
        DocIdSet result = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        BitSet bits = DocIdSets.toBitSet(result.iterator(), reader.maxDoc());
        assertThat(bits.cardinality(), equalTo(1));

        tf = new TermFilter(new Term("all", "xxx"));
        result = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        bits = DocIdSets.toBitSet(result.iterator(), reader.maxDoc());
        assertThat(bits.cardinality(), equalTo(100));

        reader.close();
        rd.close();
    }

    @Test
    public void testTermsFilter() throws Exception {
        String fieldName = "field1";
        Directory rd = new RAMDirectory();
        IndexWriter w = new IndexWriter(rd, new IndexWriterConfig(new KeywordAnalyzer()));
        for (int i = 0; i < 100; i++) {
            Document doc = new Document();
            int term = i * 10; //terms are units of 10;
            doc.add(new Field(fieldName, "" + term, StringField.TYPE_NOT_STORED));
            doc.add(new Field("all", "xxx", StringField.TYPE_NOT_STORED));
            w.addDocument(doc);
            if ((i % 40) == 0) {
                w.commit();
            }
        }
        LeafReader reader = SlowCompositeReaderWrapper.wrap(DirectoryReader.open(w, true));
        w.close();

        TermsFilter tf = new TermsFilter(new Term[]{new Term(fieldName, "19")});
        assertNull(tf.getDocIdSet(reader.getContext(), reader.getLiveDocs()));

        tf = new TermsFilter(new Term[]{new Term(fieldName, "19"), new Term(fieldName, "20")});
        DocIdSet result = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        BitSet bits = DocIdSets.toBitSet(result.iterator(), reader.maxDoc());
        assertThat(bits.cardinality(), equalTo(1));

        tf = new TermsFilter(new Term[]{new Term(fieldName, "19"), new Term(fieldName, "20"), new Term(fieldName, "10")});
        result = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        bits = DocIdSets.toBitSet(result.iterator(), reader.maxDoc());
        assertThat(bits.cardinality(), equalTo(2));

        tf = new TermsFilter(new Term[]{new Term(fieldName, "19"), new Term(fieldName, "20"), new Term(fieldName, "10"), new Term(fieldName, "00")});
        result = tf.getDocIdSet(reader.getContext(), reader.getLiveDocs());
        bits = DocIdSets.toBitSet(result.iterator(), reader.maxDoc());
        assertThat(bits.cardinality(), equalTo(2));

        reader.close();
        rd.close();
    }
}
